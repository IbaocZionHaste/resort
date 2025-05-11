package com.example.resort.addcart.data;

import java.util.Objects;

public class CartItem {
    private final String name;
    private final double price;
    private final String category;
    private int quantity;
    private Integer capacity; /// Only for Cottage and Boat
    private String imageUrl;  /// New field for item image

    /// Constructor for items that may have capacity (Cottage and Boat)
    public CartItem(String name, double price, String category, Integer capacity, String imageUrl) {
        this.name = name;
        this.price = price;
        this.category = category;
        this.quantity = 1; /// Default quantity
        this.imageUrl = imageUrl;
        /// Assign capacity ONLY if it's a "Cottage" or "Boat"
        this.capacity = ("Cottage".equals(category) || "Boat".equals(category) || "Room".equals(category)) ? capacity : null;
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

    /// Getters
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

    /// Setters
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setCapacity(Integer capacity) {
        if ("Cottage".equals(category) || "Boat".equals(category) || "Room".equals(category)) {
            this.capacity = capacity;
        }
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        CartItem other = (CartItem) obj;
        return category.equals(other.category); /// Or use ID if that's more unique
    }

    @Override
    public int hashCode() {
        return Objects.hash(category); /// Use same field as above
    }



}

///Fix Current
//package com.example.resort.addcart.data;
//
//public class CartItem {
//    private final String name;
//    private final double price;
//    private final String category;
//    private int quantity;
//    private Integer capacity; // Only for Cottage and Boat
//    private String imageUrl;  // New field for item image
//
//    /// Constructor for items that may have capacity (Cottage and Boat)
//    public CartItem(String name, double price, String category, Integer capacity, String imageUrl) {
//        this.name = name;
//        this.price = price;
//        this.category = category;
//        this.quantity = 1; // Default quantity
//        this.imageUrl = imageUrl;
//        /// Assign capacity ONLY if it's a "Cottage" or "Boat"
//        this.capacity = ("Cottage".equals(category) || "Boat".equals(category)) ? capacity : null;
//    }
//
//    // Overloaded constructor for Cottage/Boat without image
//    public CartItem(String name, double price, String category, Integer capacity) {
//        this(name, price, category, capacity, null);
//    }
//
//    // Constructor for other items (without capacity) with image
//    public CartItem(String name, double price, String category, String imageUrl) {
//        this(name, price, category, null, imageUrl);
//    }
//
//    // Constructor for other items (without capacity and without image)
//    public CartItem(String name, double price, String category) {
//        this(name, price, category, null, null);
//    }
//
//    /// Getters
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
//    public Integer getCapacity() {
//        return capacity;
//    }
//
//    public String getImageUrl() {
//        return imageUrl;
//    }
//
//    /// Setters
//    public void setQuantity(int quantity) {
//        this.quantity = quantity;
//    }
//
//    public void setCapacity(Integer capacity) {
//        if ("Cottage".equals(category) || "Boat".equals(category)) {
//            this.capacity = capacity;
//        }
//    }
//
//    public void setImageUrl(String imageUrl) {
//        this.imageUrl = imageUrl;
//    }
//
//
//}
