package com.foodie.db;

import com.foodie.model.Order;
import com.foodie.model.OrderItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Data-access object for the {@code orders} / {@code order_items} tables in Neon PostgreSQL.
 *
 * Order lifecycle: PENDING -> ACCEPTED -> PICKED_UP -> DELIVERED, with REJECTED as an
 * admin terminal state. Riders claim from a shared pool of ACCEPTED, unassigned orders.
 */
public class OrderDao {

    public static final String PENDING   = "PENDING";
    public static final String ACCEPTED  = "ACCEPTED";
    public static final String REJECTED  = "REJECTED";
    public static final String PICKED_UP = "PICKED_UP";
    public static final String DELIVERED = "DELIVERED";
    public static final String CANCELLED = "CANCELLED";

    public OrderDao() {
        try {
            ensureTables();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize OrderDao", e);
        }
    }

    private void ensureTables() throws SQLException {
        final String ordersSql =
            "CREATE TABLE IF NOT EXISTS orders (" +
            "id SERIAL PRIMARY KEY, " +
            "order_code VARCHAR(40) UNIQUE, " +
            "user_id INTEGER, " +
            "customer_name VARCHAR(255), " +
            "tenant_id INTEGER NOT NULL DEFAULT 1, " +
            "address VARCHAR(512), " +
            "phone VARCHAR(40), " +
            "email VARCHAR(255), " +
            "payment_method VARCHAR(20), " +
            "total NUMERIC(10,2) NOT NULL DEFAULT 0, " +
            "status VARCHAR(20) NOT NULL DEFAULT 'PENDING', " +
            "rider_id INTEGER, " +
            "rider_name VARCHAR(255), " +
            "delivery_pin VARCHAR(4), " +
            "table_id INTEGER, " +
            "table_name VARCHAR(120), " +
            "coupon_code VARCHAR(40), " +
            "discount NUMERIC(10,2) NOT NULL DEFAULT 0, " +
            "payment_proof VARCHAR(255), " +
            "rating INTEGER, " +
            "created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()" +
            ")";

        final String itemsSql =
            "CREATE TABLE IF NOT EXISTS order_items (" +
            "id SERIAL PRIMARY KEY, " +
            "order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE, " +
            "item_id INTEGER, " +
            "item_name VARCHAR(255), " +
            "price NUMERIC(10,2) NOT NULL DEFAULT 0, " +
            "quantity INTEGER NOT NULL DEFAULT 1" +
            ")";

        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(ordersSql);
            st.execute(itemsSql);
            // Migrations for deployments created before these columns existed.
            st.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_pin VARCHAR(4)");
            st.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS table_id INTEGER");
            st.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS table_name VARCHAR(120)");
            st.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS coupon_code VARCHAR(40)");
            st.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS discount NUMERIC(10,2) NOT NULL DEFAULT 0");
            st.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_proof VARCHAR(255)");
            st.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS rating INTEGER");
            st.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS email VARCHAR(255)");
        }
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    /**
     * Persist an order and its line items in a single transaction.
     *
     * @return the generated order id, or -1 on failure.
     */
    public int createOrder(Order order, List<OrderItem> items) throws SQLException {
        final String orderSql =
            "INSERT INTO orders (order_code, user_id, customer_name, tenant_id, address, phone, email, " +
            "payment_method, total, status, table_id, table_name, coupon_code, discount, payment_proof) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        final String itemSql =
            "INSERT INTO order_items (order_id, item_id, item_name, price, quantity) " +
            "VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            int orderId;
            try (PreparedStatement ps = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, order.getOrderCode());
                ps.setInt(2, order.getUserId());
                ps.setString(3, order.getCustomerName());
                ps.setInt(4, order.getTenantId());
                ps.setString(5, order.getAddress());
                ps.setString(6, order.getPhone());
                ps.setString(7, order.getEmail());
                ps.setString(8, order.getPaymentMethod());
                ps.setDouble(9, order.getTotal());
                ps.setString(10, PENDING);
                if (order.getTableId() > 0) {
                    ps.setInt(11, order.getTableId());
                } else {
                    ps.setNull(11, java.sql.Types.INTEGER);
                }
                ps.setString(12, order.getTableName());
                ps.setString(13, order.getCouponCode());
                ps.setDouble(14, order.getDiscount());
                ps.setString(15, order.getPaymentProof());
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        conn.rollback();
                        return -1;
                    }
                    orderId = keys.getInt(1);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                for (OrderItem oi : items) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, oi.getItemId());
                    ps.setString(3, oi.getItemName());
                    ps.setDouble(4, oi.getPrice());
                    ps.setInt(5, oi.getQuantity());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
            return orderId;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    // ---------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------

    public List<Order> findAll() throws SQLException {
        return queryOrders(
            "SELECT * FROM orders ORDER BY id DESC", null, true);
    }

    public List<Order> findByUserId(int userId) throws SQLException {
        return queryOrders(
            "SELECT * FROM orders WHERE user_id = ? ORDER BY id DESC", userId, true);
    }

    /** Rider pool: accepted orders that no rider has claimed yet. */
    public List<Order> findAvailableForRider() throws SQLException {
        return queryOrders(
            "SELECT * FROM orders WHERE status = 'ACCEPTED' AND rider_id IS NULL ORDER BY id DESC",
            null, true);
    }

    public List<Order> findByRiderId(int riderId) throws SQLException {
        return queryOrders(
            "SELECT * FROM orders WHERE rider_id = ? ORDER BY id DESC", riderId, true);
    }

    public Order findById(int id) throws SQLException {
        List<Order> list = queryOrders("SELECT * FROM orders WHERE id = ?", id, true);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Shared query helper.
     *
     * @param param optional bind parameter (Integer) — bound at position 1 when non-null.
     * @param withItems when true, eagerly loads each order's line items.
     */
    private List<Order> queryOrders(String sql, Integer param, boolean withItems) throws SQLException {
        List<Order> orders = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setInt(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapOrder(rs));
                }
            }
        }
        if (withItems && !orders.isEmpty()) {
            loadItems(orders);
        }
        return orders;
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setId(rs.getInt("id"));
        o.setOrderCode(rs.getString("order_code"));
        o.setUserId(rs.getInt("user_id"));
        o.setCustomerName(rs.getString("customer_name"));
        o.setTenantId(rs.getInt("tenant_id"));
        o.setAddress(rs.getString("address"));
        o.setPhone(rs.getString("phone"));
        o.setEmail(rs.getString("email"));
        o.setPaymentMethod(rs.getString("payment_method"));
        o.setTotal(rs.getDouble("total"));
        o.setStatus(rs.getString("status"));
        int riderId = rs.getInt("rider_id");
        o.setRiderId(rs.wasNull() ? null : riderId);
        o.setRiderName(rs.getString("rider_name"));
        o.setDeliveryPin(rs.getString("delivery_pin"));
        int tableId = rs.getInt("table_id");
        o.setTableId(rs.wasNull() ? 0 : tableId);
        o.setTableName(rs.getString("table_name"));
        o.setCouponCode(rs.getString("coupon_code"));
        o.setDiscount(rs.getDouble("discount"));
        o.setPaymentProof(rs.getString("payment_proof"));
        int rating = rs.getInt("rating");
        o.setRating(rs.wasNull() ? null : rating);
        Timestamp ts = rs.getTimestamp("created_at");
        o.setCreatedAt(ts == null ? "" : ts.toString());
        return o;
    }

    /** Batch-load line items for the given orders in one query. */
    private void loadItems(List<Order> orders) throws SQLException {
        Map<Integer, Order> byId = new LinkedHashMap<>();
        StringBuilder ids = new StringBuilder();
        for (Order o : orders) {
            byId.put(o.getId(), o);
            if (ids.length() > 0) ids.append(',');
            ids.append(o.getId());
        }

        final String sql =
            "SELECT id, order_id, item_id, item_name, price, quantity " +
            "FROM order_items WHERE order_id IN (" + ids + ") ORDER BY id";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                OrderItem oi = new OrderItem(
                    rs.getInt("id"),
                    rs.getInt("order_id"),
                    rs.getInt("item_id"),
                    rs.getString("item_name"),
                    rs.getDouble("price"),
                    rs.getInt("quantity")
                );
                Order parent = byId.get(oi.getOrderId());
                if (parent != null) {
                    parent.getItems().add(oi);
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Status transitions
    // ---------------------------------------------------------------

    /**
     * Admin accept / reject. Only applies while the order is still PENDING.
     * Accepting also assigns a unique 4-digit delivery PIN that the customer
     * hands to the rider to confirm delivery.
     */
    public boolean updateStatus(int id, String status) throws SQLException {
        if (ACCEPTED.equals(status)) {
            final String sql =
                "UPDATE orders SET status = 'ACCEPTED', delivery_pin = ? WHERE id = ? AND status = 'PENDING'";
            String pin = generateUniqueDeliveryPin();
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, pin);
                ps.setInt(2, id);
                return ps.executeUpdate() == 1;
            }
        }
        final String sql = "UPDATE orders SET status = ? WHERE id = ? AND status = 'PENDING'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Generate a 4-digit PIN not currently in use by any in-flight order
     * (ACCEPTED or PICKED_UP), so two active deliveries never share a PIN.
     * Falls back to a plain random PIN after a bounded number of attempts.
     */
    private String generateUniqueDeliveryPin() throws SQLException {
        for (int attempt = 0; attempt < 50; attempt++) {
            String pin = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
            if (!pinInUse(pin)) {
                return pin;
            }
        }
        return String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private boolean pinInUse(String pin) throws SQLException {
        final String sql =
            "SELECT 1 FROM orders WHERE delivery_pin = ? AND status IN ('ACCEPTED', 'PICKED_UP') LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pin);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Rider claims an accepted, unassigned order. Race-safe: the WHERE clause guarantees only
     * one rider can win the claim.
     *
     * @return true when this rider successfully claimed the order.
     */
    public boolean claimOrder(int id, int riderId, String riderName) throws SQLException {
        final String sql =
            "UPDATE orders SET rider_id = ?, rider_name = ?, status = 'PICKED_UP' " +
            "WHERE id = ? AND rider_id IS NULL AND status = 'ACCEPTED'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, riderId);
            ps.setString(2, riderName);
            ps.setInt(3, id);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Customer cancels their own order. Only allowed while the order is still
     * cancellable (PENDING or ACCEPTED — i.e. before a rider has picked it up).
     * The user_id + status guard makes this both ownership- and race-safe:
     * a customer can never cancel someone else's order, nor one already picked
     * up, delivered, rejected, or cancelled.
     *
     * @return true when this call actually cancelled the order.
     */
    public boolean cancelOrder(int id, int userId) throws SQLException {
        final String sql =
            "UPDATE orders SET status = 'CANCELLED' " +
            "WHERE id = ? AND user_id = ? AND status IN ('PENDING', 'ACCEPTED')";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Rider marks their own picked-up order as delivered, but only when the
     * supplied 4-digit PIN matches the one assigned at acceptance. The PIN check
     * is part of the WHERE clause, so verification is atomic: a wrong PIN simply
     * updates zero rows and returns false.
     */
    public boolean markDelivered(int id, int riderId, String pin) throws SQLException {
        final String sql =
            "UPDATE orders SET status = 'DELIVERED' " +
            "WHERE id = ? AND rider_id = ? AND status = 'PICKED_UP' AND delivery_pin = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, riderId);
            ps.setString(3, pin);
            return ps.executeUpdate() == 1;
        }
    }

    // ---------------------------------------------------------------
    // Rating
    // ---------------------------------------------------------------

    /**
     * Customer rates their own delivered order (1..5 stars). Ownership- and
     * state-safe: the WHERE clause restricts to the caller's own order and only
     * once it is DELIVERED, so a customer can neither rate someone else's order
     * nor one that hasn't been delivered. Re-rating is allowed (overwrites).
     *
     * @return true when the rating was applied.
     */
    public boolean rateOrder(int id, int userId, int rating) throws SQLException {
        if (rating < 1 || rating > 5) return false;
        final String sql =
            "UPDATE orders SET rating = ? " +
            "WHERE id = ? AND user_id = ? AND status = 'DELIVERED'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rating);
            ps.setInt(2, id);
            ps.setInt(3, userId);
            return ps.executeUpdate() == 1;
        }
    }

    // ---------------------------------------------------------------
    // Metrics
    // ---------------------------------------------------------------

    public int countAll() throws SQLException {
        return count("SELECT COUNT(*) FROM orders", null);
    }

    public int countByStatus(String status) throws SQLException {
        return count("SELECT COUNT(*) FROM orders WHERE status = ?", status);
    }

    private int count(String sql, String param) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setString(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
