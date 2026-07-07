package com.foodie.model;

/**
 * A table reservation made by a customer for a date + time window.
 *
 * Persisted in the {@code reservations} table. Lifecycle mirrors orders:
 * PENDING -> ACCEPTED / REJECTED (admin), CANCELLED (customer). An optional
 * {@code orderId} links a food order placed against this reservation.
 */
public class Reservation {
    private int id;
    private String reservationCode;   // RSV<millis>, unique
    private int userId;
    private String customerName;
    private int tableId;
    private String tableName;         // snapshot of the table name at booking time
    private String reserveDate;       // "yyyy-MM-dd"
    private String timeIn;            // "HH:mm"
    private String timeOut;           // "HH:mm"
    private int partySize;
    private String purpose;
    private String status;            // PENDING | ACCEPTED | REJECTED | CANCELLED
    private Integer orderId;          // null until a food order is attached
    private String createdAt;

    public Reservation() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getReservationCode() {
        return reservationCode;
    }

    public void setReservationCode(String reservationCode) {
        this.reservationCode = reservationCode;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public int getTableId() {
        return tableId;
    }

    public void setTableId(int tableId) {
        this.tableId = tableId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getReserveDate() {
        return reserveDate;
    }

    public void setReserveDate(String reserveDate) {
        this.reserveDate = reserveDate;
    }

    public String getTimeIn() {
        return timeIn;
    }

    public void setTimeIn(String timeIn) {
        this.timeIn = timeIn;
    }

    public String getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(String timeOut) {
        this.timeOut = timeOut;
    }

    public int getPartySize() {
        return partySize;
    }

    public void setPartySize(int partySize) {
        this.partySize = partySize;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
