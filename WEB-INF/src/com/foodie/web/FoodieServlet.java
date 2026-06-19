package com.foodie.web;

import com.foodie.db.UserDao;
import com.foodie.model.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Central front-controller for the Foodie web app.
 *
 * Authentication routes (backed by Neon PostgreSQL):
 *   GET/POST /login          – credential login
 *   GET/POST /signup         – new account registration
 *   GET/POST /forgot-password – password reset
 *   GET      /logout         – session invalidation
 */
public class FoodieServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(FoodieServlet.class.getName());

    // View paths
    private static final String VIEW_HOME           = "/WEB-INF/views/index.jsp";
    private static final String VIEW_LOGIN          = "/WEB-INF/views/login.jsp";
    private static final String VIEW_SIGNUP         = "/WEB-INF/views/signup.jsp";
    private static final String VIEW_FORGOT         = "/WEB-INF/views/forgot-password.jsp";

    private final UserDao userDao = new UserDao();

    // ---------------------------------------------------------------
    // GET
    // ---------------------------------------------------------------

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        switch (req.getServletPath()) {
            case "/login":
                if (req.getSession(false) != null && req.getSession(false).getAttribute("userId") != null) {
                    res.sendRedirect(req.getContextPath() + "/dashboard");
                } else {
                    showAuth(req, res, VIEW_LOGIN,  "");
                }
                return;
            case "/signup":          showAuth(req, res, VIEW_SIGNUP, ""); return;
            case "/forgot-password": showAuth(req, res, VIEW_FORGOT, ""); return;
            case "/logout":          logout(req, res); return;

            case "/dashboard":
                if (!ensureLoggedIn(req, res)) return;
                showDashboardForRole(req, res);
                return;

            // simple "role dashboard landing"
            case "/admin/dashboard":
                if (!ensureLoggedIn(req, res)) return;
                if (!authorizeRole(req, res, "ADMIN")) return;
                showAdminDashboard(req, res);
                return;

            case "/admin/users":
                if (!ensureLoggedIn(req, res)) return;
                if (!authorizeRole(req, res, "ADMIN")) return;
                showAdminUsers(req, res);
                return;

            case "/rider/dashboard":
                if (!ensureLoggedIn(req, res)) return;
                if (!authorizeRole(req, res, "RIDER")) return;
                showRiderDashboard(req, res);
                return;

            default:                 showHome(req, res, "");
        }
    }

    // ---------------------------------------------------------------
    // POST
    // ---------------------------------------------------------------

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        req.setCharacterEncoding(StandardCharsets.UTF_8.name());

        switch (req.getServletPath()) {
            case "/login":           handleLogin(req, res);           return;
            case "/signup":          handleSignup(req, res);          return;
            case "/forgot-password": handleForgotPassword(req, res);  return;
            case "/admin/users":     handleAdminUsersPost(req, res);  return;
            default:                 handleReservation(req, res);
        }
    }

    // ---------------------------------------------------------------
    // Auth handlers
    // ---------------------------------------------------------------

    /** POST /login – validate credentials against Neon and start a session. */
    private void handleLogin(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String email    = param(req, "email");
        String password = param(req, "password");

        if (blank(email) || blank(password)) {
            showAuth(req, res, VIEW_LOGIN, msg("error", "Email and password are required."));
            return;
        }

        try {
            User user = userDao.findByEmail(email);
            if (user != null && password.equals(user.getPassword())) {
                String normalizedRole = user.getRole() == null ? "USER" : user.getRole().trim().toUpperCase();
                if ("admin@gmail.com".equalsIgnoreCase(normalise(email))) {
                    normalizedRole = "ADMIN";
                }
                HttpSession session = req.getSession(true);
                session.setAttribute("userId",    user.getId());
                session.setAttribute("userName",  user.getName());
                session.setAttribute("userEmail", user.getEmail());
                session.setAttribute("userRole",  normalizedRole);
                session.setAttribute("tenantId",   user.getTenantId());

                res.sendRedirect(req.getContextPath() + "/dashboard");
            } else {
                showAuth(req, res, VIEW_LOGIN, msg("error", "Invalid email or password."));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Login DB error", e);
            showAuth(req, res, VIEW_LOGIN, msg("error", "A server error occurred. Please try again."));
        }
    }

    /** POST /signup – create a new account in Neon. */
    private void handleSignup(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String name     = param(req, "name");
        String email    = param(req, "email");
        String password = param(req, "password");
        String role     = param(req, "role");

        if (blank(name) || blank(email) || blank(password)) {
            showAuth(req, res, VIEW_SIGNUP, msg("error", "All fields are required."));
            return;
        }

        if (blank(role)) {
            role = "USER";
        }
        role = role.toUpperCase();
        if (!role.equals("USER") && !role.equals("RIDER") && !role.equals("ADMIN")) {
            showAuth(req, res, VIEW_SIGNUP, msg("error", "Invalid account type selected."));
            return;
        }
        if ("ADMIN".equals(role) && !"admin@gmail.com".equalsIgnoreCase(email)) {
            showAuth(req, res, VIEW_SIGNUP, msg("error", "Admin accounts can only be created for admin@gmail.com."));
            return;
        }
        if (password.length() < 8) {
            showAuth(req, res, VIEW_SIGNUP, msg("error", "Password must be at least 8 characters."));
            return;
        }

        try {
            if (userDao.emailExists(email)) {
                showAuth(req, res, VIEW_SIGNUP,
                    msg("error", "An account with this email already exists."));
                return;
            }
            boolean created = userDao.createUserWithRoleTenant(name, email, password, role, 1);
            if (created) {
                // Redirect to login with a success flash
                showAuth(req, res, VIEW_LOGIN,
                    msg("success", "Account created successfully! Please log in."));
            } else {
                showAuth(req, res, VIEW_SIGNUP,
                    msg("error", "Could not create account. Please try again."));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Signup DB error", e);
            showAuth(req, res, VIEW_SIGNUP, msg("error", "A server error occurred. Please try again."));
        }
    }

    /** POST /forgot-password – update password for an existing Neon account. */
    private void handleForgotPassword(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String email           = param(req, "email");
        String newPassword     = param(req, "new_password");
        String confirmPassword = param(req, "confirm_password");

        if (blank(email) || blank(newPassword) || blank(confirmPassword)) {
            showAuth(req, res, VIEW_FORGOT, msg("error", "All fields are required."));
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            showAuth(req, res, VIEW_FORGOT,
                msg("error", "New password and confirm password do not match."));
            return;
        }
        if (newPassword.length() < 8) {
            showAuth(req, res, VIEW_FORGOT,
                msg("error", "Password must be at least 8 characters."));
            return;
        }

        try {
            if (!userDao.emailExists(email)) {
                showAuth(req, res, VIEW_FORGOT,
                    msg("error", "No account found for that email address."));
                return;
            }
            boolean updated = userDao.updatePassword(email, newPassword);
            if (updated) {
                showAuth(req, res, VIEW_LOGIN,
                    msg("success", "Password updated successfully. Please log in."));
            } else {
                showAuth(req, res, VIEW_FORGOT,
                    msg("error", "Could not update password. Please try again."));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "ForgotPassword DB error", e);
            showAuth(req, res, VIEW_FORGOT, msg("error", "A server error occurred. Please try again."));
        }
    }

    /** GET /logout – invalidate the session and redirect to home. */
    private void logout(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        res.sendRedirect(req.getContextPath() + "/home");
    }

    // ---------------------------------------------------------------
    // Reservation handler (existing functionality)
    // ---------------------------------------------------------------

    private void handleReservation(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        Map<String, String> r = new LinkedHashMap<>();
        r.put("Name",    param(req, "full_name"));
        r.put("Email",   param(req, "email_address"));
        r.put("People",  param(req, "total_person"));
        r.put("Date",    param(req, "booking_date"));
        r.put("Message", param(req, "message"));

        showHome(req, res, buildReservationFeedback(r));
    }

    // ---------------------------------------------------------------
    // View helpers
    // ---------------------------------------------------------------

    private void showHome(HttpServletRequest req, HttpServletResponse res, String feedback)
            throws ServletException, IOException {
        req.setAttribute("reservationMessage", feedback);
        forward(req, res, VIEW_HOME);
    }

    private void showAuth(HttpServletRequest req, HttpServletResponse res,
                          String view, String feedback)
            throws ServletException, IOException {
        req.setAttribute("authMessage", feedback);
        forward(req, res, view);
    }

    private void forward(HttpServletRequest req, HttpServletResponse res, String path)
            throws ServletException, IOException {
        req.getRequestDispatcher(path).forward(req, res);
    }

    // ---------------------------------------------------------------
    // Feedback builders
    // ---------------------------------------------------------------

    /**
     * Build a styled feedback paragraph.
     *
     * @param type    "success" or "error" (maps to CSS class on .auth-message)
     * @param message plain-text message (HTML-escaped internally)
     */
    private String msg(String type, String message) {
        return "<p class=\"auth-message " + type + "\">" + escape(message) + "</p>";
    }

    private String buildReservationFeedback(Map<String, String> r) {
        if (blank(r.get("Name")) || blank(r.get("Email")) || blank(r.get("Message"))) {
            return "<p class=\"reservation-message error\">" +
                   "Please fill all required reservation details.</p>";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"reservation-message success\">");
        sb.append("<p>Thanks, ").append(escape(r.get("Name")))
          .append(". Your table request has been received.</p><ul>");
        detail(sb, "Email",  r.get("Email"));
        detail(sb, "People", r.get("People"));
        detail(sb, "Date",   r.get("Date"));
        sb.append("</ul></div>");
        return sb.toString();
    }

    private void detail(StringBuilder sb, String label, String value) {
        if (!blank(value)) {
            sb.append("<li><strong>").append(label).append(":</strong> ")
              .append(escape(value)).append("</li>");
        }
    }

    // ---------------------------------------------------------------
    // Dashboard / RBAC helpers
    // ---------------------------------------------------------------

    private void showDashboardForRole(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        // If session role wasn't set (e.g., old deployment / partial session), recover from DB.
        ensureRoleInSession(req, session);

        String role = (String) session.getAttribute("userRole");
        if (role == null) role = "USER";

        switch (role) {
            case "ADMIN":
                showAdminDashboard(req, res);
                return;
            case "RIDER":
                showRiderDashboard(req, res);
                return;
            default:
                showUserDashboard(req, res);
        }
    }

    private void showUserDashboard(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        forward(req, res, "/WEB-INF/views/dashboard.jsp");
    }

    private void showAdminUsers(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        try {
            req.setAttribute("adminUsers", userDao.findAll());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load admin users", e);
            req.setAttribute("adminMessage", msg("error", "Unable to load users. Please try again."));
        }
        forward(req, res, "/WEB-INF/views/admin/users.jsp");
    }

    private void handleAdminUsersPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!authorizeRole(req, res, "ADMIN")) return;

        String action = param(req, "action");
        String message;
        try {
            switch (action) {
                case "create":
                    message = createAdminUser(req);
                    break;
                case "update":
                    message = updateAdminUser(req);
                    break;
                case "delete":
                    message = deleteAdminUser(req);
                    break;
                default:
                    message = "Unknown admin action.";
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Admin user CRUD error", e);
            message = "A server error occurred while updating users.";
        }
        req.setAttribute("adminMessage", msg("success", message));
        showAdminUsers(req, res);
    }

    private String createAdminUser(HttpServletRequest req) throws SQLException {
        String name     = param(req, "name");
        String email    = param(req, "email");
        String password = param(req, "password");
        String role     = param(req, "role");
        String tenantId = param(req, "tenantId");

        if (blank(name) || blank(email) || blank(password) || blank(role)) {
            return "Name, email, password and role are required.";
        }
        if (password.length() < 8) {
            return "Password must be at least 8 characters.";
        }
        if (userDao.emailExists(email)) {
            return "A user with that email already exists.";
        }

        int tenant = 1;
        try {
            tenant = Integer.parseInt(tenantId);
        } catch (NumberFormatException ignored) {
        }

        boolean created = userDao.createUserWithRoleTenant(name, email, password, role.toUpperCase(), tenant);
        return created ? "User created successfully." : "Could not create user.";
    }

    private String updateAdminUser(HttpServletRequest req) throws SQLException {
        String idValue   = param(req, "id");
        String role      = param(req, "role");
        String tenantId  = param(req, "tenantId");

        if (blank(idValue) || blank(role)) {
            return "User ID and role are required to update a user.";
        }

        int id;
        int tenant = 1;
        try {
            id = Integer.parseInt(idValue);
        } catch (NumberFormatException e) {
            return "Invalid user ID.";
        }
        try {
            tenant = Integer.parseInt(tenantId);
        } catch (NumberFormatException ignored) {
        }

        boolean updated = userDao.updateRoleTenantById(id, role.toUpperCase(), tenant);
        return updated ? "User role updated successfully." : "Could not update user role.";
    }

    private String deleteAdminUser(HttpServletRequest req) throws SQLException {
        String idValue = param(req, "id");
        if (blank(idValue)) {
            return "User ID is required to delete a user.";
        }

        int id;
        try {
            id = Integer.parseInt(idValue);
        } catch (NumberFormatException e) {
            return "Invalid user ID.";
        }
        boolean deleted = userDao.deleteById(id);
        return deleted ? "User deleted successfully." : "Could not delete user.";
    }

    private boolean authorizeRole(HttpServletRequest req, HttpServletResponse res, String requiredRole)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null) {
            res.sendRedirect(req.getContextPath() + "/dashboard");
            return false;
        }
        String role = (String) session.getAttribute("userRole");
        if (role == null) {
            ensureRoleInSession(req, session);
            role = (String) session.getAttribute("userRole");
        }
        if (role == null || !requiredRole.equalsIgnoreCase(role)) {
            res.sendRedirect(req.getContextPath() + "/dashboard");
            return false;
        }
        return true;
    }

    private void ensureRoleInSession(HttpServletRequest req, HttpSession session) {
        Object roleObj = session.getAttribute("userRole");
        if (roleObj != null) return;

        Object emailObj = session.getAttribute("userEmail");
        if (emailObj == null) return;

        String email = String.valueOf(emailObj);
        try {
            User user = userDao.findByEmail(email);
            if (user != null) {
                session.setAttribute("userRole", user.getRole());
                session.setAttribute("tenantId", user.getTenantId());
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to recover role from DB", e);
        }
    }

    private void showAdminDashboard(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        forward(req, res, "/WEB-INF/views/admin/dashboard.jsp");
    }

    private void showRiderDashboard(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        forward(req, res, "/WEB-INF/views/rider/dashboard.jsp");
    }

    private boolean ensureLoggedIn(HttpServletRequest req, HttpServletResponse res) throws IOException {
        HttpSession session = req.getSession(false);
        Object userId = session == null ? null : session.getAttribute("userId");
        if (userId == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return false;
        }
        return true;
    }

    private void requireRole(HttpServletRequest req, HttpServletResponse res, String requiredRole) throws IOException {
        HttpSession session = req.getSession(false);
        String role = session == null ? null : (String) session.getAttribute("userRole");
        if (role == null || !requiredRole.equalsIgnoreCase(role)) {
            res.sendRedirect(req.getContextPath() + "/dashboard");
        }
    }


    // ---------------------------------------------------------------
    // Utility
    // ---------------------------------------------------------------

    /** Trim a request parameter; never returns null. */
    private static String param(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v == null ? "" : v.trim();
    }

    private static boolean blank(String v) {
        return v == null || v.trim().isEmpty();
    }

    private static String escape(String v) {
        if (v == null) return "";
        return v.replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&#39;");
    }
}
