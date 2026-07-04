package com.foodie.model;

public class OrderItem {
    private int id;
    private int orderId;
    private int itemId;
    private String itemName;
    private double price;
    private int quantity;

    public OrderItem() {}

    public OrderItem(int id, int orderId, int itemId, String itemName, double price, int quantity) {
        this.id = id;
        this.orderId = orderId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.price = price;
        this.quantity = quantity;
    }

    /** Convenience constructor used when building an order before it is persisted. */
    public OrderItem(int itemId, String itemName, double price, int quantity) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.price = price;
        this.quantity = quantity;
    }

    public double getLineTotal() {
        return price * quantity;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
