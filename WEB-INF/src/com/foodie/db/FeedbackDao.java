package com.foodie.db;

import com.foodie.model.Feedback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the {@code feedback} table (public footer form).
 * Follows the same self-provisioning JDBC pattern as {@link ItemDao}.
 */
public class FeedbackDao {

    public FeedbackDao() {
        try {
            ensureTable();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize FeedbackDao", e);
        }
    }

    private void ensureTable() throws SQLException {
        final String sql =
            "CREATE TABLE IF NOT EXISTS feedback (" +
            "id SERIAL PRIMARY KEY, " +
            "name VARCHAR(255) NOT NULL, " +
            "email VARCHAR(255), " +
            "message TEXT NOT NULL, " +
            "created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()" +
            ")";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    public boolean create(String name, String email, String message) throws SQLException {
        final String sql = "INSERT INTO feedback (name, email, message) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setString(2, email == null ? null : email.trim());
            ps.setString(3, message.trim());
            return ps.executeUpdate() == 1;
        }
    }

    public List<Feedback> findAll() throws SQLException {
        final String sql =
            "SELECT id, name, email, message, created_at FROM feedback ORDER BY id DESC";
        List<Feedback> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    /** Most recent feedback entries, capped at {@code limit}. */
    public List<Feedback> findRecent(int limit) throws SQLException {
        final String sql =
            "SELECT id, name, email, message, created_at FROM feedback ORDER BY id DESC LIMIT ?";
        List<Feedback> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public int countAll() throws SQLException {
        final String sql = "SELECT COUNT(*) FROM feedback";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Feedback map(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new Feedback(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("message"),
            ts == null ? "" : ts.toString()
        );
    }
}
