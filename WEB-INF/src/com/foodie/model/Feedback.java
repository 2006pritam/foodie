package com.foodie.model;

/**
 * A feedback message submitted from the public site footer form.
 * Persisted in the {@code feedback} table and shown on the admin dashboard.
 */
public class Feedback {
    private int id;
    private String name;
    private String email;
    private String message;
    private String createdAt;

    public Feedback() {}

    public Feedback(int id, String name, String email, String message, String createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.message = message;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
