package com.foodie.model;

/**
 * A discount coupon an admin creates and a customer applies at checkout.
 *
 * <p>{@code type} is either {@link #PERCENT} (value is a percentage, e.g. 20 =
 * 20% off) or {@link #FLAT} (value is a flat Rs amount off). A coupon is usable
 * only when it is active, not past its expiry date, and the cart subtotal meets
 * {@code minOrder}.</p>
 */
public class Coupon {

    public static final String PERCENT = "PERCENT";
    public static final String FLAT    = "FLAT";

    private int id;
    private String code;
    private String type;        // PERCENT or FLAT
    private double value;       // percent (0..100) or flat Rs amount
    private double minOrder;    // minimum cart subtotal required (0 = no minimum)
    private String expiryDate;  // ISO yyyy-MM-dd, null/empty = never expires
    private boolean active;
    private String createdAt;

    public Coupon() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public double getMinOrder() { return minOrder; }
    public void setMinOrder(double minOrder) { this.minOrder = minOrder; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    /** Rs discount this coupon yields for the given subtotal, capped at the subtotal. */
    public double discountFor(double subtotal) {
        double d = PERCENT.equals(type) ? subtotal * (value / 100.0) : value;
        if (d < 0) d = 0;
        if (d > subtotal) d = subtotal;
        return d;
    }
}
