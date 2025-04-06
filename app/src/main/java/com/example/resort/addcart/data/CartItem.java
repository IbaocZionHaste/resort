package com.example.resort.addcart.data;

public class CartItem {
    private final String name;
    private final double price;
    private final String category;
    private int quantity;
    private Integer capacity; // Only for Cottage and Boat
    private String imageUrl;  // New field for item image

    // Constructor for items that may have capacity (Cottage and Boat)
    public CartItem(String name, double price, String category, Integer capacity, String imageUrl) {
        this.name = name;
        this.price = price;
        this.category = category;
        this.quantity = 1; // Default quantity
        this.imageUrl = imageUrl;
        // Assign capacity ONLY if it's a "Cottage" or "Boat"
        this.capacity = ("Cottage".equals(category) || "Boat".equals(category)) ? capacity : null;
    }

    // Overloaded constructor for Cottage/Boat without image
    public CartItem(String name, double price, String category, Integer capacity) {
        this(name, price, category, capacity, null);
    }

    // Constructor for other items (without capacity) with image
    public CartItem(String name, double price, String category, String imageUrl) {
        this(name, price, category, null, imageUrl);
    }

    // Constructor for other items (without capacity and without image)
    public CartItem(String name, double price, String category) {
        this(name, price, category, null, null);
    }

    // Getters
    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public int getQuantity() {
        return quantity;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    // Setters
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setCapacity(Integer capacity) {
        if ("Cottage".equals(category) || "Boat".equals(category)) {
            this.capacity = capacity;
        }
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}


//package com.example.resort.addcart.data;
//
//public class CartItem {
//    private final String name;
//    private final double price;
//    private final String category; // New field for category
//    private int quantity;
//
//    // Constructor that accepts category
//    public CartItem(String name, double price, String category) {
//        // If you wish, you could prepend the category to the name here
//        // For example: this.name = category + ": " + name;
//        // But often it's better to handle formatting in the UI.
//        this.name = name;
//        this.price = price;
//        this.category = category;
//        this.quantity = 1; // Default quantity
//    }
//
//    // Overloaded constructor (if you want to default category to empty)
//    public CartItem(String name, double price) {
//        this(name, price, "");
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public double getPrice() {
//        return price;
//    }
//
//    public String getCategory() {
//        return category;
//    }
//
//    public int getQuantity() {
//        return quantity;
//    }
//
//    public void setQuantity(int quantity) {
//        this.quantity = quantity;
//    }
//}
