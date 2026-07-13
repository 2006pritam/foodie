package com.foodie.web;

import com.foodie.chat.ChatService;
import com.foodie.db.ComplaintDao;
import com.foodie.db.CouponDao;
import com.foodie.db.FeedbackDao;
import com.foodie.db.ItemDao;
import com.foodie.db.OrderDao;
import com.foodie.db.ReservationDao;
import com.foodie.db.TableDao;
import com.foodie.db.UserDao;
import com.foodie.model.Complaint;
import com.foodie.model.Coupon;
import com.foodie.model.DiningTable;
import com.foodie.model.Item;
import com.foodie.model.Order;
import com.foodie.model.OrderItem;
import com.foodie.model.Reservation;
import com.foodie.model.User;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

/**
 * Central front-controller for the Foodie web app.
 *
 * Authentication routes (backed by Neon PostgreSQL):
 *   GET/POST /login          – credential login
 *   GET/POST /signup         – new account registration
 *   GET/POST /forgot-password – password reset
 *   GET      /logout         – session invalidation
 */
@MultipartConfig(fileSizeThreshold = 1024 * 1024,
                 maxFileSize = 5_000_000,
                 maxRequestSize = 20_000_000)
public class FoodieServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(FoodieServlet.class.getName());

    // View paths
    private static final String VIEW_HOME           = "/WEB-INF/views/index.jsp";
    private static final String VIEW_LOGIN          = "/WEB-INF/views/login.jsp";
    private static final String VIEW_SIGNUP         = "/WEB-INF/views/signup.jsp";
    private static final String VIEW_FORGOT         = "/WEB-INF/views/forgot-password.jsp";

    private final UserDao userDao = new UserDao();
    private final ItemDao itemDao = new ItemDao();
    private final OrderDao orderDao = new OrderDao();
    private final TableDao tableDao = new TableDao();
    private final ReservationDao reservationDao = new ReservationDao();
    private final FeedbackDao feedbackDao = new FeedbackDao();
    private final ComplaintDao complaintDao = new ComplaintDao();
    private final CouponDao couponDao = new CouponDao();

    // ---------------------------------------------------------------
    // GET
    // ---------------------------------------------------------------

    /**
     * The route key used by the doGet/doPost switches. URLs are exposed with a
     * ".jsp" suffix (see {@link JspUrlFilter}); strip it here so every existing
     * {@code case "/menu"} etc. keeps matching regardless of the suffix.
     */
    private static String route(HttpServletRequest req) {
        String path = req.getServletPath();
        if (path != null && path.endsWith(".jsp")) {
            return path.substring(0, path.length() - ".jsp".length());
        }
        return path;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        switch (route(req)) {
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

            case "/admin/items":
                if (!ensureLoggedIn(req, res)) return;
                if (!authorizeRole(req, res, "ADMIN")) return;
                showAdminItems(req, res);
                return;

            case "/admin/orders":
                if (!ensureLoggedIn(req, res)) return;
                if (!authorizeRole(req, res, "ADMIN")) return;
                showAdminOrders(req, res);
                return;

            case "/rider/dashboard":
                if (!ensureLoggedIn(req, res)) return;
                if (!authorizeRole(req, res, "RIDER")) return;
                showRiderDashboard(req, res);
                return;

            case "/rider/orders":
                // Canonical rider view is the dashboard; keep this as a redirect target.
                res.sendRedirect(req.getContextPath() + "/rider/dashboard");
                return;

            // Customer shopping flow
            case "/menu":
                if (!ensureLoggedIn(req, res)) return;
                showMenu(req, res);
                return;

            case "/cart":
                if (!ensureLoggedIn(req, res)) return;
                showCart(req, res);
                return;

            case "/checkout":
                if (!ensureLoggedIn(req, res)) return;
                showCheckout(req, res);
                return;

            case "/orders":
                if (!ensureLoggedIn(req, res)) return;
                showUserOrders(req, res);
                return;

            // Table reservation flow
            case "/reservations":
                if (!ensureLoggedInForReservation(req, res)) return;
                showReservations(req, res);
                return;

            case "/reservations/my":
                if (!ensureLoggedIn(req, res)) return;
                showMyReservations(req, res);
                return;

            // Customer complaints
            case "/complaints":
                if (!ensureLoggedIn(req, res)) return;
                showComplaints(req, res);
                return;

            case "/profile":
                if (!ensureLoggedIn(req, res)) return;
                showProfile(req, res);
                return;

            case "/admin/complaints":
                if (!ensureLoggedIn(req, res)) return;
                if (!authorizeRole(req, res, "ADMIN")) return;
                showAdminComplaints(req, res);
                return;

            case "/admin/coupons":
                if (!ensureLoggedIn(req, res)) return;
                if (!authorizeRole(req, res, "ADMIN")) return;
                showAdminCoupons(req, res);
                return;

            case "/admin/tables":
                if (!ensureLoggedIn(req, res)) return;
                if (!authorizeRole(req, res, "ADMIN")) return;
                showAdminTables(req, res);
                return;

            case "/admin/reservations":
                if (!ensureLoggedIn(req, res)) return;
                if (!authorizeRole(req, res, "ADMIN")) return;
                showAdminReservations(req, res);
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

        switch (route(req)) {
            case "/login":           handleLogin(req, res);           return;
            case "/signup":          handleSignup(req, res);          return;
            case "/forgot-password": handleForgotPassword(req, res);  return;
            case "/profile":         handleProfilePost(req, res);     return;
            case "/admin/users":     handleAdminUsersPost(req, res);  return;
            case "/admin/items":     handleAdminItemsPost(req, res);  return;
            case "/admin/orders":    handleAdminOrdersPost(req, res); return;
            case "/rider/orders":    handleRiderOrdersPost(req, res); return;
            case "/cart":            handleCartPost(req, res);        return;
            case "/checkout":        handleCheckoutPost(req, res);    return;
            case "/orders":          handleUserOrdersPost(req, res);  return;
            case "/chat":            handleChat(req, res);            return;
            case "/reservations":       handleReservationsPost(req, res);      return;
            case "/admin/tables":       handleAdminTablesPost(req, res);       return;
            case "/admin/reservations": handleAdminReservationsPost(req, res); return;
            case "/complaints":         handleComplaintsPost(req, res);        return;
            case "/admin/complaints":   handleAdminComplaintsPost(req, res);   return;
            case "/admin/coupons":      handleAdminCouponsPost(req, res);      return;
            default:                 handleFeedback(req, res);
        }
    }

    /**
     * POST /chat – customer support chatbot. Accepts a "message" parameter,
     * relays it to NVIDIA server-side via {@link ChatService}, and returns a
     * small JSON payload {"reply":"..."}. Login is required.
     */
    private void handleChat(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        res.setContentType("application/json;charset=UTF-8");

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.getWriter().write("{\"reply\":\"Please sign in to use chat support.\"}");
            return;
        }

        String message = param(req, "message");
        String reply = ChatService.getInstance().reply(message);
        res.getWriter().write("{\"reply\":\"" + ChatService.jsonEscape(reply) + "\"}");
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
                if ("admin@gmail.com".equalsIgnoreCase(email)) {
                    normalizedRole = "ADMIN";
                }
                HttpSession session = req.getSession(true);
                session.setAttribute("userId",    user.getId());
                session.setAttribute("userName",  user.getName());
                session.setAttribute("userEmail", user.getEmail());
                session.setAttribute("userRole",  normalizedRole);
                session.setAttribute("tenantId",   user.getTenantId());

                // Resume the page the user was bounced from (e.g. Reservation), else the dashboard.
                Object after = session.getAttribute("afterLogin");
                session.removeAttribute("afterLogin");
                String target = (after instanceof String && "USER".equals(normalizedRole))
                    ? (String) after : "/dashboard";
                res.sendRedirect(req.getContextPath() + target);
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
    // Public feedback form (footer)
    // ---------------------------------------------------------------

    /** POST /home – persist a feedback message from the footer form. */
    private void handleFeedback(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String name    = param(req, "name");
        String email   = param(req, "email");
        String message = param(req, "message");

        if (blank(name) || blank(message)) {
            showHome(req, res, "<p class=\"reservation-message error\">"
                + "Please enter your name and a message.</p>");
            return;
        }

        try {
            feedbackDao.create(name, email, message);
            showHome(req, res, "<div class=\"reservation-message success\">"
                + "<p>Thanks, " + escape(name) + "! Your feedback has been received.</p></div>");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to save feedback", e);
            showHome(req, res, "<p class=\"reservation-message error\">"
                + "Sorry, we couldn't submit your feedback. Please try again.</p>");
        }
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
                // Customers land straight on the menu instead of the welcome dashboard.
                res.sendRedirect(req.getContextPath() + "/menu");
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

    private void showAdminItems(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        try {
            req.setAttribute("items", itemDao.findAll());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load admin items", e);
            req.setAttribute("adminItemMessage", msg("error", "Unable to load items. Please try again."));
        }
        forward(req, res, "/WEB-INF/views/admin/items.jsp");
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

    private void handleAdminItemsPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!authorizeRole(req, res, "ADMIN")) return;

        String action = param(req, "action");
        String message;
        try {
            switch (action) {
                case "create":
                    message = createAdminItem(req);
                    break;
                case "update":
                    message = updateAdminItem(req);
                    break;
                case "delete":
                    message = deleteAdminItem(req);
                    break;
                default:
                    message = "Unknown item action.";
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Admin item CRUD error", e);
            message = "A server error occurred while updating items.";
        }
        req.setAttribute("adminItemMessage", msg("success", message));
        showAdminItems(req, res);
    }

    private String createAdminItem(HttpServletRequest req) throws SQLException, IOException, ServletException {
        String name = param(req, "name");
        String category = param(req, "category");
        String priceValue = param(req, "price");
        String discountValue = param(req, "discount");

        if (blank(name) || blank(category) || blank(priceValue)) {
            return "Name, category and price are required.";
        }

        double price;
        double discount = 0;
        try {
            price = Double.parseDouble(priceValue);
            if (!blank(discountValue)) {
                discount = Double.parseDouble(discountValue);
            }
        } catch (NumberFormatException e) {
            return "Price and discount must be valid numbers.";
        }

        // Prefer a pasted web image URL (fetched directly by the browser); otherwise
        // fall back to an uploaded file. One of the two is required.
        String imageUrl = param(req, "imageUrl");
        String imagePath;
        if (!blank(imageUrl)) {
            if (!isValidImageUrl(imageUrl)) {
                return "Image URL must start with http:// or https://";
            }
            imagePath = imageUrl;
        } else {
            Part imagePart = req.getPart("image");
            if (imagePart == null || getSubmittedFileName(imagePart).isBlank()) {
                return "Please provide an image URL or upload an image for the item.";
            }
            try {
                imagePath = saveUploadedFile(imagePart, req);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to save uploaded item image", e);
                return "Could not save uploaded image.";
            }
        }

        boolean created = itemDao.createItem(name, category, price, discount, imagePath);
        return created ? "Item added successfully." : "Could not add item.";
    }

    private String updateAdminItem(HttpServletRequest req) throws SQLException, IOException, ServletException {
        String idValue = param(req, "id");
        String name = param(req, "name");
        String category = param(req, "category");
        String priceValue = param(req, "price");
        String discountValue = param(req, "discount");
        String existingImagePath = param(req, "existingImagePath");

        if (blank(idValue) || blank(name) || blank(category) || blank(priceValue)) {
            return "ID, name, category and price are required.";
        }

        int id;
        double price;
        double discount = 0;
        try {
            id = Integer.parseInt(idValue);
            price = Double.parseDouble(priceValue);
            if (!blank(discountValue)) {
                discount = Double.parseDouble(discountValue);
            }
        } catch (NumberFormatException e) {
            return "Invalid item ID, price, or discount.";
        }

        // A new web image URL wins; else a newly uploaded file; else keep the existing path.
        String imagePath = existingImagePath;
        String imageUrl = param(req, "imageUrl");
        if (!blank(imageUrl)) {
            if (!isValidImageUrl(imageUrl)) {
                return "Image URL must start with http:// or https://";
            }
            imagePath = imageUrl;
        } else {
            Part imagePart = req.getPart("image");
            if (imagePart != null && !getSubmittedFileName(imagePart).isBlank()) {
                try {
                    imagePath = saveUploadedFile(imagePart, req);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to save uploaded item image", e);
                    return "Could not save uploaded image.";
                }
            }
        }

        boolean updated = itemDao.updateItem(id, name, category, price, discount, imagePath);
        return updated ? "Item updated successfully." : "Could not update item.";
    }

    private String deleteAdminItem(HttpServletRequest req) throws SQLException {
        String idValue = param(req, "id");
        if (blank(idValue)) {
            return "Item ID is required to delete an item.";
        }
        int id;
        try {
            id = Integer.parseInt(idValue);
        } catch (NumberFormatException e) {
            return "Invalid item ID.";
        }
        boolean deleted = itemDao.deleteById(id);
        return deleted ? "Item deleted successfully." : "Could not delete item.";
    }

    private String saveUploadedFile(Part part, HttpServletRequest req) throws IOException {
        return saveUploadedFile(part, req, "assets/images/items");
    }

    private String saveUploadedFile(Part part, HttpServletRequest req, String relativeDir) throws IOException {
        String filename = getSubmittedFileName(part);
        if (filename == null || filename.isBlank()) {
            throw new IOException("No file name present.");
        }

        String extension = "";
        int dot = filename.lastIndexOf('.');
        if (dot >= 0) {
            extension = filename.substring(dot);
        }

        String savedName = System.currentTimeMillis() + "_" + Math.abs(filename.hashCode()) + extension;
        String uploadPath = req.getServletContext().getRealPath(relativeDir);
        if (uploadPath == null) {
            throw new IOException("Unable to resolve upload directory.");
        }

        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            throw new IOException("Failed to create image upload directory.");
        }

        Path target = uploadDir.toPath().resolve(savedName);
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return relativeDir + "/" + savedName;
    }

    private String getSubmittedFileName(Part part) {
        if (part == null) {
            return null;
        }
        String header = part.getHeader("content-disposition");
        if (header == null) {
            return null;
        }
        for (String segment : header.split(";")) {
            if (segment.trim().startsWith("filename=")) {
                String filename = segment.substring(segment.indexOf('=') + 1).trim().replace("\"", "");
                return filename.substring(filename.lastIndexOf(File.separator) + 1);
            }
        }
        return null;
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
        try {
            List<Order> all = orderDao.findAll();
            req.setAttribute("totalItems",     itemDao.countAll());
            req.setAttribute("totalOrders",    all.size());
            req.setAttribute("pendingCount",   orderDao.countByStatus(OrderDao.PENDING));
            req.setAttribute("acceptedCount",  orderDao.countByStatus(OrderDao.ACCEPTED));
            req.setAttribute("pickedUpCount",  orderDao.countByStatus(OrderDao.PICKED_UP));
            req.setAttribute("deliveredCount", orderDao.countByStatus(OrderDao.DELIVERED));
            req.setAttribute("rejectedCount",  orderDao.countByStatus(OrderDao.REJECTED));
            req.setAttribute("recentOrders",   all.subList(0, Math.min(5, all.size())));
            req.setAttribute("feedbackCount",  feedbackDao.countAll());
            req.setAttribute("recentFeedback", feedbackDao.findRecent(8));
            req.setAttribute("openComplaints", complaintDao.countByStatus(ComplaintDao.OPEN));
            req.setAttribute("activeCoupons",  couponDao.countActive());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load admin dashboard metrics", e);
        }
        forward(req, res, "/WEB-INF/views/admin/dashboard.jsp");
    }

    private void showRiderDashboard(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        // Surface a one-shot flash set by a prior POST (e.g. delivery PIN result).
        if (session != null) {
            Object flash = session.getAttribute("riderFlash");
            if (flash != null) {
                req.setAttribute("riderMessage", flash);
                session.removeAttribute("riderFlash");
            }
        }
        int riderId = intAttr(session, "userId");
        try {
            req.setAttribute("availableOrders", orderDao.findAvailableForRider());
            req.setAttribute("myOrders",        orderDao.findByRiderId(riderId));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load rider orders", e);
            req.setAttribute("riderMessage", msg("error", "Unable to load orders. Please try again."));
        }
        forward(req, res, "/WEB-INF/views/rider/dashboard.jsp");
    }

    // ---------------------------------------------------------------
    // Admin orders
    // ---------------------------------------------------------------

    private void showAdminOrders(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        try {
            req.setAttribute("orders", orderDao.findAll());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load admin orders", e);
            req.setAttribute("adminOrderMessage", msg("error", "Unable to load orders. Please try again."));
        }
        forward(req, res, "/WEB-INF/views/admin/orders.jsp");
    }

    private void handleAdminOrdersPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!authorizeRole(req, res, "ADMIN")) return;

        String action = param(req, "action");
        int id = parseIntSafe(param(req, "id"), -1);
        try {
            if (id > 0 && "accept".equals(action)) {
                orderDao.updateStatus(id, OrderDao.ACCEPTED);
            } else if (id > 0 && "reject".equals(action)) {
                orderDao.updateStatus(id, OrderDao.REJECTED);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Admin order status update error", e);
        }
        res.sendRedirect(req.getContextPath() + "/admin/orders");
    }

    // ---------------------------------------------------------------
    // Rider orders
    // ---------------------------------------------------------------

    private void handleRiderOrdersPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!authorizeRole(req, res, "RIDER")) return;

        HttpSession session = req.getSession(false);
        int riderId = intAttr(session, "userId");
        String riderName = session == null ? "Rider" : String.valueOf(session.getAttribute("userName"));
        String action = param(req, "action");
        int id = parseIntSafe(param(req, "id"), -1);
        try {
            if (id > 0 && "pickup".equals(action)) {
                orderDao.claimOrder(id, riderId, riderName);
            } else if (id > 0 && "deliver".equals(action)) {
                String pin = param(req, "pin");
                boolean delivered = orderDao.markDelivered(id, riderId, pin);
                session.setAttribute("riderFlash", delivered
                    ? msg("success", "Delivery confirmed. Order marked as delivered.")
                    : msg("error", "Incorrect delivery PIN. Ask the customer for the 4-digit PIN and try again."));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Rider order action error", e);
        }
        res.sendRedirect(req.getContextPath() + "/rider/dashboard");
    }

    // ---------------------------------------------------------------
    // Customer shopping flow (menu / cart / checkout / orders)
    // ---------------------------------------------------------------

    private void showMenu(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        try {
            req.setAttribute("items", itemDao.findAll());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load menu", e);
            req.setAttribute("menuMessage", msg("error", "Unable to load the menu. Please try again."));
        }
        req.setAttribute("cartCount", cartCount(getCart(req.getSession(true))));
        forward(req, res, "/WEB-INF/views/menu.jsp");
    }

    private void showCart(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        populateCartAttributes(req);
        forward(req, res, "/WEB-INF/views/cart.jsp");
    }

    private void showCheckout(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        List<OrderItem> lines = populateCartAttributes(req);
        if (lines.isEmpty()) {
            res.sendRedirect(req.getContextPath() + "/menu");
            return;
        }
        applyCouponAttributes(req);
        forward(req, res, "/WEB-INF/views/checkout.jsp");
    }

    /**
     * Resolve the coupon currently held in the session (if any) against the live
     * cart subtotal, and expose couponCode / couponDiscount / payableTotal to the
     * checkout view. Re-validated every render so a coupon that no longer applies
     * (cart shrank below its minimum, expired, deactivated) is quietly dropped.
     */
    private void applyCouponAttributes(HttpServletRequest req) {
        HttpSession session = req.getSession(true);
        double subtotal = round2(toDouble(req.getAttribute("cartTotal")));
        String code = (String) session.getAttribute("checkoutCoupon");
        double discount = 0;
        String applied = null;

        if (code != null) {
            try {
                Coupon c = couponDao.findUsable(code, subtotal);
                if (c != null) {
                    discount = round2(c.discountFor(subtotal));
                    applied = c.getCode();
                } else {
                    // No longer valid — forget it so the user isn't misled.
                    session.removeAttribute("checkoutCoupon");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Coupon revalidation failed", e);
                session.removeAttribute("checkoutCoupon");
            }
        }

        req.setAttribute("couponCode", applied);
        req.setAttribute("couponDiscount", discount);
        req.setAttribute("payableTotal", round2(Math.max(0, subtotal - discount)));
    }

    /**
     * Apply or remove a coupon on the checkout screen. Stores just the code in
     * the session; the actual discount is always recomputed from the DB so it
     * can't be tampered with. Sets a checkoutMessage for user feedback.
     */
    private void handleCouponAction(HttpServletRequest req, String action) {
        HttpSession session = req.getSession(true);
        if ("remove_coupon".equals(action)) {
            session.removeAttribute("checkoutCoupon");
            req.setAttribute("checkoutMessage", msg("success", "Coupon removed."));
            return;
        }
        // apply_coupon
        String code = param(req, "coupon_code");
        if (blank(code)) {
            req.setAttribute("checkoutMessage", msg("error", "Enter a coupon code."));
            return;
        }
        code = code.trim().toUpperCase();
        // Compute the subtotal straight from the cart — the cartTotal request
        // attribute isn't populated yet at this point in the POST flow.
        double subtotal;
        try {
            List<OrderItem> lines = buildCartLines(getCart(session));
            double t = 0;
            for (OrderItem line : lines) t += line.getLineTotal();
            subtotal = round2(t);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Coupon subtotal calc error", e);
            req.setAttribute("checkoutMessage", msg("error", "A server error occurred applying the coupon."));
            return;
        }
        try {
            Coupon c = couponDao.findUsable(code, subtotal);
            if (c == null) {
                session.removeAttribute("checkoutCoupon");
                req.setAttribute("checkoutMessage",
                    msg("error", "That coupon is invalid, expired, or your order doesn't meet its minimum."));
            } else {
                session.setAttribute("checkoutCoupon", c.getCode());
                req.setAttribute("checkoutMessage",
                    msg("success", "Coupon " + c.getCode() + " applied — you save Rs "
                        + String.format("%.2f", c.discountFor(subtotal)) + "."));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Coupon apply error", e);
            req.setAttribute("checkoutMessage", msg("error", "A server error occurred applying the coupon."));
        }
    }

    private static double toDouble(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0.0;
    }

    private void showUserOrders(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        // Surface a one-shot flash set by a prior POST (e.g. order cancellation).
        HttpSession session = req.getSession(false);
        if (session != null) {
            Object flash = session.getAttribute("orderFlash");
            if (flash != null) {
                req.setAttribute("orderMessage", flash);
                session.removeAttribute("orderFlash");
            }
        }
        int userId = intAttr(session, "userId");
        try {
            req.setAttribute("orders", orderDao.findByUserId(userId));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load user orders", e);
            req.setAttribute("orderMessage", msg("error", "Unable to load your orders. Please try again."));
        }
        forward(req, res, "/WEB-INF/views/orders.jsp");
    }

    /** POST /orders – customer actions on their own orders (currently: cancel). */
    private void handleUserOrdersPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!ensureLoggedIn(req, res)) return;

        int userId = intAttr(req.getSession(false), "userId");
        String action = param(req, "action");
        int orderId = parseIntSafe(param(req, "orderId"), -1);

        if (orderId > 0 && "cancel".equals(action)) {
            try {
                boolean cancelled = orderDao.cancelOrder(orderId, userId);
                req.getSession(true).setAttribute("orderFlash", cancelled
                    ? msg("success", "Your order was cancelled.")
                    : msg("error", "This order can no longer be cancelled."));
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Order cancel error", e);
                req.getSession(true).setAttribute("orderFlash",
                    msg("error", "A server error occurred while cancelling your order."));
            }
        } else if (orderId > 0 && "rate".equals(action)) {
            int rating = parseIntSafe(param(req, "rating"), 0);
            try {
                boolean rated = orderDao.rateOrder(orderId, userId, rating);
                req.getSession(true).setAttribute("orderFlash", rated
                    ? msg("success", "Thanks for rating your order!")
                    : msg("error", "That order can't be rated yet."));
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Order rating error", e);
                req.getSession(true).setAttribute("orderFlash",
                    msg("error", "A server error occurred while saving your rating."));
            }
        }
        res.sendRedirect(req.getContextPath() + "/orders");
    }

    private void handleCartPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!ensureLoggedIn(req, res)) return;

        Map<Integer, Integer> cart = getCart(req.getSession(true));
        String action = param(req, "action");
        int itemId = parseIntSafe(param(req, "itemId"), -1);
        int qty    = parseIntSafe(param(req, "quantity"), 1);

        switch (action) {
            case "add":
                if (itemId > 0) {
                    if (qty < 1) qty = 1;
                    cart.merge(itemId, qty, Integer::sum);
                }
                break;
            case "update":
                if (itemId > 0) {
                    if (qty <= 0) cart.remove(itemId);
                    else cart.put(itemId, qty);
                }
                break;
            case "remove":
                if (itemId > 0) cart.remove(itemId);
                break;
            case "clear":
                cart.clear();
                break;
            default:
                break;
        }
        res.sendRedirect(req.getContextPath() + "/cart");
    }

    private void handleCheckoutPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!ensureLoggedIn(req, res)) return;

        HttpSession session = req.getSession(true);
        Map<Integer, Integer> cart = getCart(session);
        if (cart.isEmpty()) {
            res.sendRedirect(req.getContextPath() + "/menu");
            return;
        }

        // Coupon apply/remove happen inline on the checkout page (not order placement):
        // stash/clear the code in the session, then re-render checkout with the result.
        String action = param(req, "action");
        if ("apply_coupon".equals(action) || "remove_coupon".equals(action)) {
            handleCouponAction(req, action);
            showCheckout(req, res);
            return;
        }

        String address = param(req, "address");
        String phone   = param(req, "phone");
        String payment = param(req, "payment_method");

        // Dine-in context: an order started from a table reservation. The table
        // stands in for a delivery address, so address isn't required here.
        int resvTableId      = intAttr(session, "resvTableId");
        String resvTableName = (String) session.getAttribute("resvTableName");
        int resvId           = intAttr(session, "resvId");
        boolean dineIn       = resvTableId > 0 && resvTableName != null;
        if (dineIn && blank(address)) {
            address = "Dine-in — Table " + resvTableName;
        }

        boolean addressOk = dineIn || !blank(address);
        if (!addressOk || blank(phone) || blank(payment)) {
            req.setAttribute("checkoutMessage", msg("error", "Address, phone and payment method are required."));
            showCheckout(req, res);
            return;
        }

        // UPI: a payment screenshot must be uploaded (the front-end enforces the
        // 60s window; the server enforces that proof is actually present).
        String paymentProof = null;
        if ("UPI".equals(payment)) {
            try {
                Part proofPart = req.getPart("payment_proof");
                if (proofPart == null || getSubmittedFileName(proofPart).isBlank()) {
                    req.setAttribute("checkoutMessage",
                        msg("error", "UPI payment failed — please scan the QR and upload your payment screenshot."));
                    showCheckout(req, res);
                    return;
                }
                paymentProof = saveUploadedFile(proofPart, req, "assets/images/payments");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to save UPI payment screenshot", e);
                req.setAttribute("checkoutMessage",
                    msg("error", "We couldn't save your payment screenshot. Please try again."));
                showCheckout(req, res);
                return;
            }
        }

        try {
            // Re-price every line from the database — never trust cart-side prices.
            List<OrderItem> lines = buildCartLines(cart);
            if (lines.isEmpty()) {
                cart.clear();
                res.sendRedirect(req.getContextPath() + "/menu");
                return;
            }

            double total = 0;
            for (OrderItem line : lines) total += line.getLineTotal();
            double subtotal = round2(total);

            // Re-validate any applied coupon server-side against the real subtotal;
            // never trust a client-supplied discount.
            double discount = 0;
            String couponCode = null;
            String sessionCoupon = (String) session.getAttribute("checkoutCoupon");
            if (sessionCoupon != null) {
                Coupon c = couponDao.findUsable(sessionCoupon, subtotal);
                if (c != null) {
                    discount = round2(c.discountFor(subtotal));
                    couponCode = c.getCode();
                }
            }
            double payable = round2(Math.max(0, subtotal - discount));

            Order order = new Order();
            order.setOrderCode("ORD" + System.currentTimeMillis());
            order.setUserId(intAttr(session, "userId"));
            order.setCustomerName(String.valueOf(session.getAttribute("userName")));
            order.setTenantId(intAttr(session, "tenantId") > 0 ? intAttr(session, "tenantId") : 1);
            order.setAddress(address);
            order.setPhone(phone);
            order.setPaymentMethod(payment);
            order.setCouponCode(couponCode);
            order.setDiscount(discount);
            order.setPaymentProof(paymentProof);
            order.setTotal(payable);
            if (dineIn) {
                order.setTableId(resvTableId);
                order.setTableName(resvTableName);
            }
            // Mock payment always succeeds.

            int newId = orderDao.createOrder(order, lines);
            if (newId > 0) {
                cart.clear();
                session.removeAttribute("checkoutCoupon");
                if (dineIn) {
                    // Link the order to its reservation and clear the dine-in context.
                    if (resvId > 0) {
                        try { reservationDao.linkOrder(resvId, newId); }
                        catch (SQLException e) { LOGGER.log(Level.WARNING, "Failed to link order to reservation", e); }
                    }
                    session.removeAttribute("resvTableId");
                    session.removeAttribute("resvTableName");
                    session.removeAttribute("resvId");
                }
                // Land on My Orders with this order's receipt auto-opened for easy printout.
                String placed = java.net.URLEncoder.encode(order.getOrderCode(), StandardCharsets.UTF_8);
                res.sendRedirect(req.getContextPath() + "/orders?placed=" + placed);
            } else {
                req.setAttribute("checkoutMessage", msg("error", "Could not place your order. Please try again."));
                showCheckout(req, res);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Checkout error", e);
            req.setAttribute("checkoutMessage", msg("error", "A server error occurred while placing your order."));
            showCheckout(req, res);
        }
    }

    // ---------------------------------------------------------------
    // Table reservation flow
    // ---------------------------------------------------------------

    /** Like {@link #ensureLoggedIn} but remembers to resume /reservations after login. */
    private boolean ensureLoggedInForReservation(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        HttpSession session = req.getSession(false);
        Object userId = session == null ? null : session.getAttribute("userId");
        if (userId == null) {
            req.getSession(true).setAttribute("afterLogin", "/reservations");
            res.sendRedirect(req.getContextPath() + "/login");
            return false;
        }
        return true;
    }

    /** Customer booking screen: shape grid + per-table status for a chosen date/time window. */
    private void showReservations(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String date    = param(req, "date");
        String timeIn  = param(req, "timeIn");
        String timeOut = param(req, "timeOut");
        if (blank(date))    date    = java.time.LocalDate.now().toString();
        if (blank(timeIn))  timeIn  = "18:00";
        if (blank(timeOut)) timeOut = "20:00";

        HttpSession session = req.getSession(false);
        Object flash = session == null ? null : session.getAttribute("resvFlash");
        if (flash != null) { req.setAttribute("reservationMessage", flash); session.removeAttribute("resvFlash"); }

        try {
            List<DiningTable> tables = tableDao.findAllActive();
            List<Reservation> active = reservationDao.findActiveByDate(date);

            String today = java.time.LocalDate.now().toString();
            String now   = java.time.LocalTime.now().toString();   // "HH:mm:ss"
            String nowHm = now.length() >= 5 ? now.substring(0, 5) : now;

            Map<Integer, String>  statusByTable = new LinkedHashMap<>();
            Map<Integer, Integer> seatsByTable  = new LinkedHashMap<>();
            for (DiningTable t : tables) {
                String status = "FREE";
                int seats = 0;
                for (Reservation r : active) {
                    if (r.getTableId() != t.getId()) continue;
                    // Overlap with the selected window.
                    if (r.getTimeIn().compareTo(timeOut) < 0 && r.getTimeOut().compareTo(timeIn) > 0) {
                        seats = Math.max(seats, r.getPartySize());
                        boolean occupiedNow = "ACCEPTED".equals(r.getStatus())
                            && date.equals(today)
                            && r.getTimeIn().compareTo(nowHm) <= 0 && r.getTimeOut().compareTo(nowHm) > 0;
                        status = occupiedNow ? "OCCUPIED" : "BOOKED";
                        if (occupiedNow) break;   // occupied wins
                    }
                }
                statusByTable.put(t.getId(), status);
                seatsByTable.put(t.getId(), seats);
            }

            req.setAttribute("tables", tables);
            req.setAttribute("statusByTable", statusByTable);
            req.setAttribute("seatsByTable", seatsByTable);
            req.setAttribute("resvDate", date);
            req.setAttribute("resvTimeIn", timeIn);
            req.setAttribute("resvTimeOut", timeOut);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load reservation screen", e);
            req.setAttribute("reservationMessage", msg("error", "Unable to load tables. Please try again."));
        }
        forward(req, res, "/WEB-INF/views/reservations.jsp");
    }

    private void showMyReservations(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object flash = session == null ? null : session.getAttribute("resvFlash");
        if (flash != null) { req.setAttribute("reservationMessage", flash); session.removeAttribute("resvFlash"); }
        int userId = intAttr(session, "userId");
        try {
            req.setAttribute("reservations", reservationDao.findByUserId(userId));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load user reservations", e);
            req.setAttribute("reservationMessage", msg("error", "Unable to load your reservations. Please try again."));
        }
        forward(req, res, "/WEB-INF/views/my-reservations.jsp");
    }

    /** POST /reservations – create a booking (optionally continuing to food ordering) or cancel one. */
    private void handleReservationsPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!ensureLoggedIn(req, res)) return;

        HttpSession session = req.getSession(true);
        int userId = intAttr(session, "userId");
        String action = param(req, "action");

        if ("cancel".equals(action)) {
            int id = parseIntSafe(param(req, "id"), -1);
            try {
                if (id > 0) reservationDao.cancel(id, userId);
                session.setAttribute("resvFlash", msg("success", "Reservation cancelled."));
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Reservation cancel error", e);
                session.setAttribute("resvFlash", msg("error", "Could not cancel the reservation."));
            }
            res.sendRedirect(req.getContextPath() + "/reservations/my");
            return;
        }

        // action = create
        int tableId    = parseIntSafe(param(req, "tableId"), -1);
        String date    = param(req, "date");
        String timeIn  = param(req, "timeIn");
        String timeOut = param(req, "timeOut");
        int partySize  = parseIntSafe(param(req, "partySize"), 0);
        String purpose = param(req, "purpose");
        boolean withFood = "1".equals(param(req, "withFood"));

        if (tableId <= 0 || blank(date) || blank(timeIn) || blank(timeOut) || partySize <= 0) {
            session.setAttribute("resvFlash", msg("error", "Please choose a table, date, time and party size."));
            res.sendRedirect(req.getContextPath() + "/reservations?date=" + urlenc(date));
            return;
        }
        if (timeIn.compareTo(timeOut) >= 0) {
            session.setAttribute("resvFlash", msg("error", "Time-out must be after time-in."));
            res.sendRedirect(req.getContextPath() + "/reservations?date=" + urlenc(date));
            return;
        }

        try {
            DiningTable table = tableDao.findById(tableId);
            if (table == null || !table.isActive()) {
                session.setAttribute("resvFlash", msg("error", "That table is not available."));
                res.sendRedirect(req.getContextPath() + "/reservations?date=" + urlenc(date));
                return;
            }
            if (partySize > table.getCapacity()) {
                session.setAttribute("resvFlash",
                    msg("error", "Party size exceeds the table capacity (" + table.getCapacity() + ")."));
                res.sendRedirect(req.getContextPath() + "/reservations?date=" + urlenc(date));
                return;
            }
            if (!reservationDao.isTableAvailable(tableId, date, timeIn, timeOut)) {
                session.setAttribute("resvFlash",
                    msg("error", "Sorry, that table was just booked for this time. Pick another."));
                res.sendRedirect(req.getContextPath() + "/reservations?date=" + urlenc(date));
                return;
            }

            Reservation r = new Reservation();
            r.setReservationCode("RSV" + System.currentTimeMillis());
            r.setUserId(userId);
            r.setCustomerName(String.valueOf(session.getAttribute("userName")));
            r.setTableId(tableId);
            r.setTableName(table.getTableName());
            r.setReserveDate(date);
            r.setTimeIn(timeIn);
            r.setTimeOut(timeOut);
            r.setPartySize(partySize);
            r.setPurpose(purpose);

            int newId = reservationDao.create(r);
            if (newId <= 0) {
                session.setAttribute("resvFlash", msg("error", "Could not create the reservation. Please try again."));
                res.sendRedirect(req.getContextPath() + "/reservations?date=" + urlenc(date));
                return;
            }

            if (withFood) {
                // Carry the table context into the food-ordering flow.
                session.setAttribute("resvId", newId);
                session.setAttribute("resvTableId", tableId);
                session.setAttribute("resvTableName", table.getTableName());
                session.setAttribute("resvFlash",
                    msg("success", "Table " + table.getTableName() + " reserved. Now add food to your order."));
                res.sendRedirect(req.getContextPath() + "/menu");
            } else {
                session.setAttribute("resvFlash",
                    msg("success", "Table " + table.getTableName() + " reserved. Awaiting confirmation."));
                res.sendRedirect(req.getContextPath() + "/reservations/my");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Reservation create error", e);
            session.setAttribute("resvFlash", msg("error", "A server error occurred. Please try again."));
            res.sendRedirect(req.getContextPath() + "/reservations?date=" + urlenc(date));
        }
    }

    // ---------------------------------------------------------------
    // Admin: table catalog + reservation approvals
    // ---------------------------------------------------------------

    private void showAdminTables(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        try {
            req.setAttribute("tables", tableDao.findAll());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load admin tables", e);
            req.setAttribute("adminTableMessage", msg("error", "Unable to load tables. Please try again."));
        }
        forward(req, res, "/WEB-INF/views/admin/tables.jsp");
    }

    private void handleAdminTablesPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!authorizeRole(req, res, "ADMIN")) return;

        String action = param(req, "action");
        String message;
        try {
            switch (action) {
                case "create": message = createAdminTable(req); break;
                case "update": message = updateAdminTable(req); break;
                case "delete": message = deleteAdminTable(req); break;
                default:       message = "Unknown table action.";
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Admin table CRUD error", e);
            message = "A server error occurred while updating tables.";
        }
        req.setAttribute("adminTableMessage", msg("success", message));
        showAdminTables(req, res);
    }

    private String createAdminTable(HttpServletRequest req) throws SQLException {
        String name  = param(req, "table_name");
        String shape = normalizeShape(param(req, "shape"));
        String floor = normalizeFloor(param(req, "floor"));
        String zone  = param(req, "zone");
        int capacity = parseIntSafe(param(req, "capacity"), 0);
        if (blank(name) || capacity <= 0) {
            return "Table name and a capacity of at least 1 are required.";
        }
        boolean created = tableDao.createTable(name, shape, capacity, floor, zone);
        return created ? "Table added successfully." : "Could not add table.";
    }

    private String updateAdminTable(HttpServletRequest req) throws SQLException {
        int id = parseIntSafe(param(req, "id"), -1);
        String name  = param(req, "table_name");
        String shape = normalizeShape(param(req, "shape"));
        String floor = normalizeFloor(param(req, "floor"));
        String zone  = param(req, "zone");
        int capacity = parseIntSafe(param(req, "capacity"), 0);
        boolean active = "on".equalsIgnoreCase(param(req, "active")) || "true".equalsIgnoreCase(param(req, "active"));
        if (id <= 0 || blank(name) || capacity <= 0) {
            return "Valid table id, name and capacity are required.";
        }
        boolean updated = tableDao.updateTable(id, name, shape, capacity, floor, zone, active);
        return updated ? "Table updated successfully." : "Could not update table.";
    }

    private String deleteAdminTable(HttpServletRequest req) throws SQLException {
        int id = parseIntSafe(param(req, "id"), -1);
        if (id <= 0) return "Valid table id is required to delete a table.";
        boolean deleted = tableDao.deleteById(id);
        return deleted ? "Table deleted successfully." : "Could not delete table.";
    }

    private void showAdminReservations(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        try {
            req.setAttribute("reservations", reservationDao.findAll());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load admin reservations", e);
            req.setAttribute("adminReservationMessage", msg("error", "Unable to load reservations. Please try again."));
        }
        forward(req, res, "/WEB-INF/views/admin/reservations.jsp");
    }

    private void handleAdminReservationsPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!authorizeRole(req, res, "ADMIN")) return;

        String action = param(req, "action");
        int id = parseIntSafe(param(req, "id"), -1);
        try {
            if (id > 0 && "accept".equals(action)) {
                reservationDao.updateStatus(id, ReservationDao.ACCEPTED);
            } else if (id > 0 && "reject".equals(action)) {
                reservationDao.updateStatus(id, ReservationDao.REJECTED);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Admin reservation status error", e);
        }
        res.sendRedirect(req.getContextPath() + "/admin/reservations");
    }

    // ---------------------------------------------------------------
    // Customer complaints
    // ---------------------------------------------------------------

    /** Customer "raise a complaint" screen: their orders to pick from + their complaints. */
    private void showComplaints(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object flash = session == null ? null : session.getAttribute("complaintFlash");
        if (flash != null) { req.setAttribute("complaintMessage", flash); session.removeAttribute("complaintFlash"); }

        int userId = intAttr(session, "userId");
        try {
            req.setAttribute("orders", orderDao.findByUserId(userId));
            req.setAttribute("complaints", complaintDao.findByUserId(userId));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load complaints screen", e);
            req.setAttribute("complaintMessage", msg("error", "Unable to load your complaints. Please try again."));
        }
        forward(req, res, "/WEB-INF/views/complaints.jsp");
    }

    /** POST /complaints – customer raises a complaint against one of their own orders. */
    private void handleComplaintsPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!ensureLoggedIn(req, res)) return;

        HttpSession session = req.getSession(true);
        int userId = intAttr(session, "userId");
        int orderId = parseIntSafe(param(req, "orderId"), -1);
        String message = param(req, "message");

        if (orderId <= 0 || blank(message)) {
            session.setAttribute("complaintFlash", msg("error", "Please choose an order and describe the problem."));
            res.sendRedirect(req.getContextPath() + "/complaints");
            return;
        }

        try {
            // Verify the order belongs to this user before accepting the complaint.
            Order order = orderDao.findById(orderId);
            if (order == null || order.getUserId() != userId) {
                session.setAttribute("complaintFlash", msg("error", "That order was not found in your account."));
                res.sendRedirect(req.getContextPath() + "/complaints");
                return;
            }

            Complaint c = new Complaint();
            c.setComplaintCode("CMP" + System.currentTimeMillis());
            c.setUserId(userId);
            c.setCustomerName(String.valueOf(session.getAttribute("userName")));
            c.setOrderId(orderId);
            c.setOrderCode(order.getOrderCode());
            c.setMessage(message);
            complaintDao.create(c);

            session.setAttribute("complaintFlash",
                msg("success", "Your complaint about order " + order.getOrderCode() + " has been submitted."));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to save complaint", e);
            session.setAttribute("complaintFlash", msg("error", "A server error occurred. Please try again."));
        }
        res.sendRedirect(req.getContextPath() + "/complaints");
    }

    private void showAdminComplaints(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        try {
            req.setAttribute("complaints", complaintDao.findAll());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load admin complaints", e);
            req.setAttribute("adminComplaintMessage", msg("error", "Unable to load complaints. Please try again."));
        }
        forward(req, res, "/WEB-INF/views/admin/complaints.jsp");
    }

    private void handleAdminComplaintsPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!authorizeRole(req, res, "ADMIN")) return;

        int id = parseIntSafe(param(req, "id"), -1);
        try {
            if (id > 0 && "resolve".equals(param(req, "action"))) {
                String reply = param(req, "reply");
                complaintDao.resolve(id, reply);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Admin complaint resolve error", e);
        }
        res.sendRedirect(req.getContextPath() + "/admin/complaints");
    }

    // ---------------------------------------------------------------
    // Admin — Coupons
    // ---------------------------------------------------------------

    private void showAdminCoupons(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object flash = session == null ? null : session.getAttribute("couponFlash");
        if (flash != null) {
            req.setAttribute("adminCouponMessage", flash);
            session.removeAttribute("couponFlash");
        }
        try {
            req.setAttribute("coupons", couponDao.findAll());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load coupons", e);
            req.setAttribute("adminCouponMessage", msg("error", "Unable to load coupons. Please try again."));
        }
        forward(req, res, "/WEB-INF/views/admin/coupons.jsp");
    }

    /** POST /admin/coupons – create / toggle-active / delete a coupon. */
    private void handleAdminCouponsPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!authorizeRole(req, res, "ADMIN")) return;

        String action = param(req, "action");
        HttpSession session = req.getSession(true);
        try {
            if ("create".equals(action)) {
                String code = param(req, "code");
                String type = param(req, "type");
                if (code != null) code = code.trim().toUpperCase();
                if (!Coupon.PERCENT.equals(type) && !Coupon.FLAT.equals(type)) {
                    type = Coupon.PERCENT;
                }
                double value    = parseDoubleSafe(param(req, "value"), -1);
                double minOrder = parseDoubleSafe(param(req, "min_order"), 0);
                String expiry   = param(req, "expiry_date");   // yyyy-MM-dd or blank

                String problem = validateCoupon(code, type, value, minOrder);
                if (problem != null) {
                    session.setAttribute("couponFlash", msg("error", problem));
                } else {
                    boolean ok = couponDao.create(code, type, value, minOrder,
                                                  blank(expiry) ? null : expiry);
                    session.setAttribute("couponFlash", ok
                        ? msg("success", "Coupon " + code + " created.")
                        : msg("error", "A coupon with that code already exists."));
                }
            } else if ("toggle".equals(action)) {
                int id = parseIntSafe(param(req, "id"), -1);
                if (id > 0) couponDao.toggleActive(id);
            } else if ("delete".equals(action)) {
                int id = parseIntSafe(param(req, "id"), -1);
                if (id > 0) couponDao.deleteById(id);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Admin coupon error", e);
            session.setAttribute("couponFlash", msg("error", "A server error occurred. Please try again."));
        }
        res.sendRedirect(req.getContextPath() + "/admin/coupons");
    }

    /** Validate coupon creation inputs; returns an error message or null when valid. */
    private String validateCoupon(String code, String type, double value, double minOrder) {
        if (blank(code)) return "Coupon code is required.";
        if (!code.matches("[A-Z0-9_-]{3,40}")) {
            return "Code must be 3–40 characters: letters, digits, '-' or '_'.";
        }
        if (value <= 0) return "Discount value must be greater than zero.";
        if (Coupon.PERCENT.equals(type) && value > 100) {
            return "A percentage discount cannot exceed 100%.";
        }
        if (minOrder < 0) return "Minimum order cannot be negative.";
        return null;
    }


    /** Show the user's profile page with current details and forms to update them. */
    private void showProfile(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object flash = session == null ? null : session.getAttribute("profileFlash");
        if (flash != null) {
            req.setAttribute("profileMessage", flash);
            session.removeAttribute("profileFlash");
        }

        int userId = intAttr(session, "userId");
        try {
            User user = userDao.findById(userId);
            if (user != null) {
                req.setAttribute("profileUser", user);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load profile", e);
            req.setAttribute("profileMessage", msg("error", "Unable to load profile. Please try again."));
        }
        forward(req, res, "/WEB-INF/views/profile.jsp");
    }

    /** POST /profile – handle profile updates (name, phone, photo, password, email). */
    private void handleProfilePost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        if (!ensureLoggedIn(req, res)) return;

        HttpSession session = req.getSession(true);
        int userId = intAttr(session, "userId");
        String action = param(req, "action");
        String message = null;

        try {
            switch (action) {
                case "updateProfile": {
                    String name = param(req, "name");
                    String phone = param(req, "phone");
                    String photoUrl = param(req, "photoUrl");
                    boolean updated = userDao.updateProfile(userId, name, phone, photoUrl);
                    message = updated ? "Profile updated successfully." : "Profile update failed.";
                    if (updated && name != null && !name.trim().isEmpty()) {
                        session.setAttribute("userName", name.trim());
                    }
                    break;
                }
                case "changePassword": {
                    String currentPassword = param(req, "currentPassword");
                    String newPassword = param(req, "newPassword");
                    String confirmPassword = param(req, "confirmPassword");

                    if (blank(currentPassword) || blank(newPassword)) {
                        message = "All password fields are required.";
                    } else if (!newPassword.equals(confirmPassword)) {
                        message = "New passwords do not match.";
                    } else {
                        User user = userDao.findById(userId);
                        if (user != null && user.getPassword().equals(currentPassword)) {
                            boolean updated = userDao.updatePassword(user.getEmail(), newPassword);
                            message = updated ? "Password changed successfully." : "Password change failed.";
                        } else {
                            message = "Current password is incorrect.";
                        }
                    }
                    break;
                }
                case "changeEmail": {
                    String newEmail = param(req, "newEmail");
                    String password = param(req, "password");

                    if (blank(newEmail) || blank(password)) {
                        message = "Email and current password are required.";
                    } else {
                        User user = userDao.findById(userId);
                        if (user != null && user.getPassword().equals(password)) {
                            try {
                                boolean updated = userDao.updateEmail(userId, newEmail);
                                if (updated) {
                                    session.setAttribute("userEmail", newEmail.toLowerCase());
                                    message = "Email updated successfully.";
                                } else {
                                    message = "Email update failed.";
                                }
                            } catch (SQLException e) {
                                if ("Email already in use".equals(e.getMessage())) {
                                    message = "This email is already registered.";
                                } else {
                                    throw e;
                                }
                            }
                        } else {
                            message = "Current password is incorrect.";
                        }
                    }
                    break;
                }
                default:
                    message = "Unknown action.";
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Profile update error", e);
            message = "A server error occurred. Please try again.";
        }

        if (message != null) {
            session.setAttribute("profileFlash", msg(message.startsWith("Password") || message.startsWith("Email") || message.startsWith("Profile") ? "success" : "error", message));
        }
        res.sendRedirect(req.getContextPath() + "/profile");
    }

    private static String normalizeShape(String s) {
        String v = s == null ? "" : s.trim().toUpperCase();
        switch (v) {
            case "SQUARE": case "RECTANGLE": case "CIRCLE": case "FAMILY": return v;
            default: return "SQUARE";
        }
    }

    private static String normalizeFloor(String s) {
        String v = s == null ? "" : s.trim().toUpperCase();
        switch (v) {
            case "GROUND": case "FIRST": case "SECOND": case "ROOF": return v;
            default: return "GROUND";
        }
    }

    private static String urlenc(String v) {
        return java.net.URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------
    // Cart helpers
    // ---------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<Integer, Integer> getCart(HttpSession session) {
        Object c = session.getAttribute("cart");
        if (c instanceof Map) {
            return (Map<Integer, Integer>) c;
        }
        Map<Integer, Integer> cart = new LinkedHashMap<>();
        session.setAttribute("cart", cart);
        return cart;
    }

    private int cartCount(Map<Integer, Integer> cart) {
        int total = 0;
        for (int q : cart.values()) total += q;
        return total;
    }

    /** Join cart entries to live item rows and compute discounted line prices. */
    private List<OrderItem> buildCartLines(Map<Integer, Integer> cart) throws SQLException {
        List<OrderItem> lines = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : cart.entrySet()) {
            int qty = e.getValue();
            if (qty <= 0) continue;
            Item item = itemDao.findById(e.getKey());
            if (item == null) continue;
            lines.add(new OrderItem(item.getId(), item.getName(),
                                    round2(discountedPrice(item)), qty));
        }
        return lines;
    }

    /** Build cart line/total request attributes for the cart & checkout views. */
    private List<OrderItem> populateCartAttributes(HttpServletRequest req)
            throws ServletException, IOException {
        Map<Integer, Integer> cart = getCart(req.getSession(true));
        List<OrderItem> lines = new ArrayList<>();
        double total = 0;
        try {
            lines = buildCartLines(cart);
            for (OrderItem line : lines) total += line.getLineTotal();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to build cart", e);
            req.setAttribute("cartMessage", msg("error", "Unable to load your cart. Please try again."));
        }
        req.setAttribute("cartLines", lines);
        req.setAttribute("cartTotal", round2(total));
        req.setAttribute("cartCount", cartCount(cart));
        return lines;
    }

    private static double discountedPrice(Item item) {
        double p = item.getPrice() * (1 - item.getDiscount() / 100.0);
        return p < 0 ? 0 : p;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static int intAttr(HttpSession session, String name) {
        if (session == null) return 0;
        Object v = session.getAttribute(name);
        if (v instanceof Number) return ((Number) v).intValue();
        return parseIntSafe(v == null ? "" : String.valueOf(v), 0);
    }

    private static int parseIntSafe(String v, int fallback) {
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return fallback;
        }
    }

    private static double parseDoubleSafe(String v, double fallback) {
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return fallback;
        }
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

    /** True when the string looks like a usable absolute web image URL. */
    private static boolean isValidImageUrl(String url) {
        if (url == null) return false;
        String u = url.trim().toLowerCase();
        return u.startsWith("http://") || u.startsWith("https://");
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
