package com.foodie.db;

import com.foodie.model.Complaint;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for {@code complaints}. A customer raises a complaint
 * against one of their orders; the admin views and resolves them.
 * Follows the same self-provisioning JDBC pattern as {@link ItemDao}.
 */
public class ComplaintDao {

    public static final String OPEN     = "OPEN";
    public static final String RESOLVED = "RESOLVED";

    public ComplaintDao() {
        try {
            ensureTable();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize ComplaintDao", e);
        }
    }

    private void ensureTable() throws SQLException {
        final String sql =
            "CREATE TABLE IF NOT EXISTS complaints (" +
            "id SERIAL PRIMARY KEY, " +
            "complaint_code VARCHAR(40) UNIQUE, " +
            "user_id INTEGER, " +
            "customer_name VARCHAR(255), " +
            "order_id INTEGER, " +
            "order_code VARCHAR(40), " +
            "message TEXT NOT NULL, " +
            "status VARCHAR(20) NOT NULL DEFAULT 'OPEN', " +
            "admin_reply TEXT, " +
            "created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()" +
            ")";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
            // Migration for deployments created before admin replies existed.
            st.execute("ALTER TABLE complaints ADD COLUMN IF NOT EXISTS admin_reply TEXT");
        }
    }

    public int create(Complaint c) throws SQLException {
        final String sql =
            "INSERT INTO complaints (complaint_code, user_id, customer_name, order_id, " +
            "order_code, message, status) VALUES (?, ?, ?, ?, ?, ?, 'OPEN')";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getComplaintCode());
            ps.setInt(2, c.getUserId());
            ps.setString(3, c.getCustomerName());
            ps.setInt(4, c.getOrderId());
            ps.setString(5, c.getOrderCode());
            ps.setString(6, c.getMessage().trim());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public List<Complaint> findByUserId(int userId) throws SQLException {
        return query("SELECT * FROM complaints WHERE user_id = ? ORDER BY id DESC", userId);
    }

    public List<Complaint> findAll() throws SQLException {
        return query("SELECT * FROM complaints ORDER BY id DESC", null);
    }

    private List<Complaint> query(String sql, Integer param) throws SQLException {
        List<Complaint> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public int countByStatus(String status) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM complaints WHERE status = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Admin marks a complaint resolved with a reply message for the customer. */
    public boolean resolve(int id, String reply) throws SQLException {
        final String sql =
            "UPDATE complaints SET status = 'RESOLVED', admin_reply = ? WHERE id = ? AND status = 'OPEN'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reply == null ? null : reply.trim());
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    private Complaint map(ResultSet rs) throws SQLException {
        Complaint c = new Complaint();
        c.setId(rs.getInt("id"));
        c.setComplaintCode(rs.getString("complaint_code"));
        c.setUserId(rs.getInt("user_id"));
        c.setCustomerName(rs.getString("customer_name"));
        c.setOrderId(rs.getInt("order_id"));
        c.setOrderCode(rs.getString("order_code"));
        c.setMessage(rs.getString("message"));
        c.setStatus(rs.getString("status"));
        c.setAdminReply(rs.getString("admin_reply"));
        Timestamp ts = rs.getTimestamp("created_at");
        c.setCreatedAt(ts == null ? "" : ts.toString());
        return c;
    }
}
