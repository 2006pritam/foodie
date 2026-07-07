package com.foodie.db;

import com.foodie.model.DiningTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the {@code restaurant_tables} catalog (admin-managed).
 * Follows the same self-provisioning JDBC pattern as {@link ItemDao}.
 */
public class TableDao {

    public TableDao() {
        try {
            ensureTable();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize TableDao", e);
        }
    }

    private void ensureTable() throws SQLException {
        final String sql =
            "CREATE TABLE IF NOT EXISTS restaurant_tables (" +
            "id SERIAL PRIMARY KEY, " +
            "table_name VARCHAR(60) NOT NULL, " +
            "shape VARCHAR(20) NOT NULL DEFAULT 'SQUARE', " +
            "capacity INTEGER NOT NULL DEFAULT 2, " +
            "floor VARCHAR(20) NOT NULL DEFAULT 'GROUND', " +
            "zone VARCHAR(60), " +
            "active BOOLEAN NOT NULL DEFAULT TRUE, " +
            "created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()" +
            ")";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    /** All active tables, ordered for a stable floor-plan layout. */
    public List<DiningTable> findAllActive() throws SQLException {
        return query(
            "SELECT id, table_name, shape, capacity, floor, zone, active " +
            "FROM restaurant_tables WHERE active = TRUE ORDER BY floor, id");
    }

    /** Every table (admin management view), newest first. */
    public List<DiningTable> findAll() throws SQLException {
        return query(
            "SELECT id, table_name, shape, capacity, floor, zone, active " +
            "FROM restaurant_tables ORDER BY id DESC");
    }

    private List<DiningTable> query(String sql) throws SQLException {
        List<DiningTable> tables = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tables.add(map(rs));
            }
        }
        return tables;
    }

    public DiningTable findById(int id) throws SQLException {
        final String sql =
            "SELECT id, table_name, shape, capacity, floor, zone, active " +
            "FROM restaurant_tables WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    private DiningTable map(ResultSet rs) throws SQLException {
        return new DiningTable(
            rs.getInt("id"),
            rs.getString("table_name"),
            rs.getString("shape"),
            rs.getInt("capacity"),
            rs.getString("floor"),
            rs.getString("zone"),
            rs.getBoolean("active")
        );
    }

    public int countAll() throws SQLException {
        final String sql = "SELECT COUNT(*) FROM restaurant_tables";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public boolean createTable(String name, String shape, int capacity, String floor, String zone)
            throws SQLException {
        final String sql =
            "INSERT INTO restaurant_tables (table_name, shape, capacity, floor, zone) " +
            "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setString(2, shape);
            ps.setInt(3, capacity);
            ps.setString(4, floor);
            ps.setString(5, zone == null ? null : zone.trim());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateTable(int id, String name, String shape, int capacity, String floor,
                               String zone, boolean active) throws SQLException {
        final String sql =
            "UPDATE restaurant_tables SET table_name = ?, shape = ?, capacity = ?, floor = ?, " +
            "zone = ?, active = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setString(2, shape);
            ps.setInt(3, capacity);
            ps.setString(4, floor);
            ps.setString(5, zone == null ? null : zone.trim());
            ps.setBoolean(6, active);
            ps.setInt(7, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteById(int id) throws SQLException {
        final String sql = "DELETE FROM restaurant_tables WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }
}
