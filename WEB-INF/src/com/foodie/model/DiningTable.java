package com.foodie.model;

/**
 * A physical restaurant table in the reservation system.
 *
 * Persisted in the {@code restaurant_tables} table (the plain name "table" is a
 * reserved SQL word, hence the qualified name). Managed by the admin: shape,
 * seat capacity (chairs), floor and an optional zone/purpose tag.
 */
public class DiningTable {
    private int id;
    private String tableName;   // display name / number, e.g. "T1", "VIP-2"
    private String shape;       // SQUARE | RECTANGLE | CIRCLE | FAMILY
    private int capacity;       // number of chairs
    private String floor;       // GROUND | FIRST | SECOND | ROOF
    private String zone;        // optional tag, e.g. VIP / Terrace / Family
    private boolean active;

    public DiningTable() {}

    public DiningTable(int id, String tableName, String shape, int capacity,
                       String floor, String zone, boolean active) {
        this.id = id;
        this.tableName = tableName;
        this.shape = shape;
        this.capacity = capacity;
        this.floor = floor;
        this.zone = zone;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
