package com.foodie.model;

/**
 * A complaint a customer raises against one of their orders.
 * Persisted in the {@code complaints} table; shown to the customer in their
 * complaints list and to the admin in the complaints panel.
 */
public class Complaint {
    private int id;
    private String complaintCode;   // CMP<millis>, unique
    private int userId;
    private String customerName;
    private int orderId;
    private String orderCode;        // snapshot of the order code at submit time
    private String message;
    private String status;           // OPEN | RESOLVED
    private String createdAt;

    public Complaint() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getComplaintCode() {
        return complaintCode;
    }

    public void setComplaintCode(String complaintCode) {
        this.complaintCode = complaintCode;
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

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
