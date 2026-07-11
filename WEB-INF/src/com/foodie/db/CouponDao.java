package com.foodie.db;

import com.foodie.model.Coupon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the {@code coupons} table in Neon PostgreSQL.
 *
 * <p>Self-bootstraps its table in the constructor, matching the other DAOs.
 * Admins create/list/toggle/delete coupons; checkout validates a code with
 * {@link #findUsable(String, double)} which enforces active + not-expired +
 * minimum-order in a single query.</p>
 */
public class CouponDao {

    public CouponDao() {
        try {
            ensureTables();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize CouponDao", e);
        }
    }

    private void ensureTables() throws SQLException {
        final String sql =
            "CREATE TABLE IF NOT EXISTS coupons (" +
            "id SERIAL PRIMARY KEY, " +
            "code VARCHAR(40) UNIQUE NOT NULL, " +
            "type VARCHAR(10) NOT NULL DEFAULT 'PERCENT', " +
            "value NUMERIC(10,2) NOT NULL DEFAULT 0, " +
            "min_order NUMERIC(10,2) NOT NULL DEFAULT 0, " +
            "expiry_date DATE, " +
            "active BOOLEAN NOT NULL DEFAULT TRUE, " +
            "created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()" +
            ")";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    // ---------------------------------------------------------------
    // Create / update / delete
    // ---------------------------------------------------------------

    /**
     * Insert a new coupon.
     *
     * @return true on success; false if the code already exists (unique violation).
     */
    public boolean create(String code, String type, double value, double minOrder, String expiryDate)
            throws SQLException {
        final String sql =
            "INSERT INTO coupons (code, type, value, min_order, expiry_date, active) " +
            "VALUES (?, ?, ?, ?, ?, TRUE)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setString(2, type);
            ps.setDouble(3, value);
            ps.setDouble(4, minOrder);
            if (expiryDate == null || expiryDate.isEmpty()) {
                ps.setNull(5, java.sql.Types.DATE);
            } else {
                ps.setDate(5, java.sql.Date.valueOf(expiryDate));
            }
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            // 23505 = unique_violation (duplicate code)
            if ("23505".equals(e.getSQLState())) {
                return false;
            }
            throw e;
        }
    }

    public boolean toggleActive(int id) throws SQLException {
        final String sql = "UPDATE coupons SET active = NOT active WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteById(int id) throws SQLException {
        final String sql = "DELETE FROM coupons WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    // ---------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------

    public List<Coupon> findAll() throws SQLException {
        final String sql = "SELECT * FROM coupons ORDER BY id DESC";
        List<Coupon> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    /**
     * Look up a coupon by code that is usable right now for the given subtotal:
     * active, not expired, and subtotal meets its minimum order.
     *
     * @return the coupon, or null if no such usable coupon exists.
     */
    public Coupon findUsable(String code, double subtotal) throws SQLException {
        final String sql =
            "SELECT * FROM coupons WHERE UPPER(code) = UPPER(?) AND active = TRUE " +
            "AND (expiry_date IS NULL OR expiry_date >= ?) AND min_order <= ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
            ps.setDouble(3, subtotal);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int countActive() throws SQLException {
        final String sql =
            "SELECT COUNT(*) FROM coupons WHERE active = TRUE " +
            "AND (expiry_date IS NULL OR expiry_date >= CURRENT_DATE)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Coupon map(ResultSet rs) throws SQLException {
        Coupon c = new Coupon();
        c.setId(rs.getInt("id"));
        c.setCode(rs.getString("code"));
        c.setType(rs.getString("type"));
        c.setValue(rs.getDouble("value"));
        c.setMinOrder(rs.getDouble("min_order"));
        java.sql.Date exp = rs.getDate("expiry_date");
        c.setExpiryDate(exp == null ? null : exp.toString());
        c.setActive(rs.getBoolean("active"));
        Timestamp ts = rs.getTimestamp("created_at");
        c.setCreatedAt(ts == null ? "" : ts.toString());
        return c;
    }
}
