package com.foodie.db;

import com.foodie.model.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ItemDao {

    private static final Logger LOGGER = Logger.getLogger(ItemDao.class.getName());

    public ItemDao() {
        try {
            ensureItemsTable();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "ItemDao initialization skipped because the database is unavailable.", e);
        }
    }

    private void ensureItemsTable() throws SQLException {
        final String sql =
            "CREATE TABLE IF NOT EXISTS items (" +
            "id SERIAL PRIMARY KEY, " +
            "name VARCHAR(255) NOT NULL, " +
            "category VARCHAR(255), " +
            "price NUMERIC(10,2) NOT NULL DEFAULT 0, " +
            "discount NUMERIC(5,2) NOT NULL DEFAULT 0, " +
            "image_path VARCHAR(512), " +
            "created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()" +
            ")";

        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    public List<Item> findAll() throws SQLException {
        final String sql =
            "SELECT id, name, category, price, discount, image_path " +
            "FROM items ORDER BY id DESC";

        List<Item> items = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(new Item(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getDouble("price"),
                    rs.getDouble("discount"),
                    rs.getString("image_path")
                ));
            }
        }
        return items;
    }

    public Item findById(int id) throws SQLException {
        final String sql =
            "SELECT id, name, category, price, discount, image_path FROM items WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Item(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("price"),
                        rs.getDouble("discount"),
                        rs.getString("image_path")
                    );
                }
            }
        }
        return null;
    }

    public int countAll() throws SQLException {
        final String sql = "SELECT COUNT(*) FROM items";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public boolean createItem(String name, String category, double price, double discount, String imagePath)
            throws SQLException {
        final String sql =
            "INSERT INTO items (name, category, price, discount, image_path) " +
            "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setString(2, category.trim());
            ps.setDouble(3, price);
            ps.setDouble(4, discount);
            ps.setString(5, imagePath);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateItem(int id, String name, String category, double price, double discount, String imagePath)
            throws SQLException {
        final String sql =
            "UPDATE items SET name = ?, category = ?, price = ?, discount = ?, image_path = ? " +
            "WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setString(2, category.trim());
            ps.setDouble(3, price);
            ps.setDouble(4, discount);
            ps.setString(5, imagePath);
            ps.setInt(6, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteById(int id) throws SQLException {
        final String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }
}
