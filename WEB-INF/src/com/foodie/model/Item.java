package com.foodie.model;

public class Item {
    private int id;
    private String name;
    private String category;
    private double price;
    private double discount;
    private String imagePath;

    public Item() {}

    public Item(int id, String name, String category, double price, double discount, String imagePath) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.discount = discount;
        this.imagePath = imagePath;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
