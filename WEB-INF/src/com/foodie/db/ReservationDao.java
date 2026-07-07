package com.foodie.db;

import com.foodie.model.Reservation;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for {@code reservations}. Whole-table bookings for a date +
 * time window; availability is enforced by an overlap check. Lifecycle mirrors
 * orders (PENDING -> ACCEPTED/REJECTED, CANCELLED).
 */
public class ReservationDao {

    public static final String PENDING   = "PENDING";
    public static final String ACCEPTED  = "ACCEPTED";
    public static final String REJECTED  = "REJECTED";
    public static final String CANCELLED = "CANCELLED";

    public ReservationDao() {
        try {
            ensureTable();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize ReservationDao", e);
        }
    }

    private void ensureTable() throws SQLException {
        final String sql =
            "CREATE TABLE IF NOT EXISTS reservations (" +
            "id SERIAL PRIMARY KEY, " +
            "reservation_code VARCHAR(40) UNIQUE, " +
            "user_id INTEGER, " +
            "customer_name VARCHAR(255), " +
            "table_id INTEGER, " +
            "table_name VARCHAR(120), " +
            "reserve_date DATE, " +
            "time_in VARCHAR(5), " +
            "time_out VARCHAR(5), " +
            "party_size INTEGER NOT NULL DEFAULT 1, " +
            "purpose VARCHAR(120), " +
            "status VARCHAR(20) NOT NULL DEFAULT 'PENDING', " +
            "order_id INTEGER, " +
            "created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()" +
            ")";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    /**
     * True when the table is free for the requested window (no PENDING/ACCEPTED
     * reservation on the same date overlaps [timeIn, timeOut)).
     */
    public boolean isTableAvailable(int tableId, String date, String timeIn, String timeOut)
            throws SQLException {
        final String sql =
            "SELECT 1 FROM reservations " +
            "WHERE table_id = ? AND reserve_date = ? AND status IN ('PENDING', 'ACCEPTED') " +
            "AND time_in < ? AND time_out > ? LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tableId);
            ps.setDate(2, Date.valueOf(date));
            ps.setString(3, timeOut);   // existing.time_in < requested.time_out
            ps.setString(4, timeIn);    // existing.time_out > requested.time_in
            try (ResultSet rs = ps.executeQuery()) {
                return !rs.next();
            }
        }
    }

    /** Insert a new reservation (status PENDING). Returns the generated id or -1. */
    public int create(Reservation r) throws SQLException {
        final String sql =
            "INSERT INTO reservations (reservation_code, user_id, customer_name, table_id, " +
            "table_name, reserve_date, time_in, time_out, party_size, purpose, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getReservationCode());
            ps.setInt(2, r.getUserId());
            ps.setString(3, r.getCustomerName());
            ps.setInt(4, r.getTableId());
            ps.setString(5, r.getTableName());
            ps.setDate(6, Date.valueOf(r.getReserveDate()));
            ps.setString(7, r.getTimeIn());
            ps.setString(8, r.getTimeOut());
            ps.setInt(9, r.getPartySize());
            ps.setString(10, r.getPurpose());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public List<Reservation> findByUserId(int userId) throws SQLException {
        return query("SELECT * FROM reservations WHERE user_id = ? ORDER BY id DESC", userId);
    }

    public List<Reservation> findAll() throws SQLException {
        return query("SELECT * FROM reservations ORDER BY id DESC", null);
    }

    /** Active (PENDING/ACCEPTED) reservations for a given date — used to compute grid status. */
    public List<Reservation> findActiveByDate(String date) throws SQLException {
        final String sql =
            "SELECT * FROM reservations WHERE reserve_date = ? AND status IN ('PENDING', 'ACCEPTED') " +
            "ORDER BY id DESC";
        List<Reservation> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private List<Reservation> query(String sql, Integer param) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setInt(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private Reservation map(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getInt("id"));
        r.setReservationCode(rs.getString("reservation_code"));
        r.setUserId(rs.getInt("user_id"));
        r.setCustomerName(rs.getString("customer_name"));
        r.setTableId(rs.getInt("table_id"));
        r.setTableName(rs.getString("table_name"));
        Date d = rs.getDate("reserve_date");
        r.setReserveDate(d == null ? "" : d.toString());
        r.setTimeIn(rs.getString("time_in"));
        r.setTimeOut(rs.getString("time_out"));
        r.setPartySize(rs.getInt("party_size"));
        r.setPurpose(rs.getString("purpose"));
        r.setStatus(rs.getString("status"));
        int orderId = rs.getInt("order_id");
        r.setOrderId(rs.wasNull() ? null : orderId);
        Timestamp ts = rs.getTimestamp("created_at");
        r.setCreatedAt(ts == null ? "" : ts.toString());
        return r;
    }

    /** Admin accept / reject. Only applies while the reservation is still PENDING. */
    public boolean updateStatus(int id, String status) throws SQLException {
        final String sql = "UPDATE reservations SET status = ? WHERE id = ? AND status = 'PENDING'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    /** Customer cancels their own reservation while it is still PENDING or ACCEPTED. */
    public boolean cancel(int id, int userId) throws SQLException {
        final String sql =
            "UPDATE reservations SET status = 'CANCELLED' " +
            "WHERE id = ? AND user_id = ? AND status IN ('PENDING', 'ACCEPTED')";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        }
    }

    /** Attach a placed food order to the reservation. */
    public boolean linkOrder(int reservationId, int orderId) throws SQLException {
        final String sql = "UPDATE reservations SET order_id = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, reservationId);
            return ps.executeUpdate() == 1;
        }
    }
}
