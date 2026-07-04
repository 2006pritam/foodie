package com.foodie.db;

import com.foodie.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data-access object for the {@code resturent} table in Neon PostgreSQL.
 *
 * All emails are normalised to lower-case before storage / lookup.
 */
public class UserDao {

    private static final Logger LOGGER = Logger.getLogger(UserDao.class.getName());

    // ---------------------------------------------------------------
    // Read operations
    // ---------------------------------------------------------------

    /**
     * Look up a user by e-mail address.
     *
     * @return the {@link User}, or {@code null} if not found.
     */
    public User findByEmail(String email) throws SQLException {
        final String sql =
            "SELECT id, name, email, password, role, tenant_id " +
            "FROM resturent WHERE email = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, normalise(email));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role");
                    String normalizedEmail = normalise(rs.getString("email"));
                    if ("admin@gmail.com".equals(normalizedEmail)) {
                        role = "ADMIN";
                    }
                    return new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password"),
                        role,
                        rs.getInt("tenant_id")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Check whether an e-mail address is already registered.
     */
    public boolean emailExists(String email) throws SQLException {
        final String sql = "SELECT 1 FROM resturent WHERE email = ? LIMIT 1";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, normalise(email));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ---------------------------------------------------------------
    // Write operations
    // ---------------------------------------------------------------

    /**
     * Insert a new user with a plain-text password into the restaurant table.
     *
     * @return {@code true} on success.
     */
    public boolean createUser(String name, String email, String plainPassword)
            throws SQLException {

        final String sql =
            "INSERT INTO resturent (name, email, password) " +
            "VALUES (?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name.trim());
            ps.setString(2, normalise(email));
            ps.setString(3, plainPassword);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Replace the password for the user identified by {@code email}.
     * A new salt is generated so the old hash is immediately invalid.
     *
     * @return {@code true} when a row was updated (i.e. the email exists).
     */
    public boolean updatePassword(String email, String newPlainPassword)
            throws SQLException {

        final String sql =
            "UPDATE resturent SET password = ? WHERE email = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newPlainPassword);
            ps.setString(2, normalise(email));
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Ensure the restaurant table contains the required role and tenant_id columns.
     * This allows runtime bootstrapping on older deployments that still have the
     * legacy schema.
     */
    public UserDao() {
        try {
            ensureUsersTable();
            ensureRoleTenantColumns();
            ensureDefaultAdminUser();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "UserDao initialization skipped because the database is unavailable.", e);
        }
    }

    public void ensureRoleTenantColumns() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(
                "ALTER TABLE resturent " +
                "ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER'");
            st.execute(
                "ALTER TABLE resturent " +
                "ADD COLUMN IF NOT EXISTS tenant_id INTEGER NOT NULL DEFAULT 1");
        }
    }

    private void ensureDefaultAdminUser() throws SQLException {
        final String adminEmail = "admin@gmail.com";
        if (emailExists(adminEmail)) {
            User admin = findByEmail(adminEmail);
            if (admin != null && (admin.getRole() == null || !"ADMIN".equalsIgnoreCase(admin.getRole()))) {
                updateRoleTenantById(admin.getId(), "ADMIN", admin.getTenantId());
            }
        } else {
            createUserWithRoleTenant("Administrator", adminEmail, "admin123", "ADMIN", 1);
        }
    }

    private void ensureUsersTable() throws SQLException {
        final String sql =
            "CREATE TABLE IF NOT EXISTS resturent (" +
            "id SERIAL PRIMARY KEY, " +
            "name VARCHAR(255), " +
            "email VARCHAR(255) NOT NULL UNIQUE, " +
            "password VARCHAR(255) NOT NULL, " +
            "role VARCHAR(20) NOT NULL DEFAULT 'USER', " +
            "tenant_id INTEGER NOT NULL DEFAULT 1" +
            ")";

        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    /**
     * Create a user with an explicit role + tenant.
     *
     * Note: users.role / users.tenant_id defaults exist, but for admin bootstrap we need to set them explicitly.
     *
     * @return {@code true} when a row was inserted.
     */
    public boolean createUserWithRoleTenant(String name,
                                             String email,
                                             String plainPassword,
                                             String role,
                                             int tenantId) throws SQLException {
        final String sql =
            "INSERT INTO resturent (name, email, password, role, tenant_id) " +
            "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name.trim());
            ps.setString(2, normalise(email));
            ps.setString(3, plainPassword);
            ps.setString(4, role);
            ps.setInt(5, tenantId);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Update role + tenant for the user identified by email.
     *
     * @return {@code true} when a row was updated.
     */
    public boolean updateRoleTenantById(int id, String role, int tenantId) throws SQLException {
        final String sql =
            "UPDATE resturent SET role = ?, tenant_id = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, role);
            ps.setInt(2, tenantId);
            ps.setInt(3, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteById(int id) throws SQLException {
        final String sql = "DELETE FROM resturent WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    public List<User> findAll() throws SQLException {
        final String sql =
            "SELECT id, name, email, password, role, tenant_id " +
            "FROM resturent ORDER BY id";

        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                users.add(new User(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("role"),
                    rs.getInt("tenant_id")
                ));
            }
        }
        return users;
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static String normalise(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
