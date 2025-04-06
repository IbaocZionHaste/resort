package com.example.resort.review.data;

public class Review {
    private final String user;
    private final int rate;
    private final String comment;
    private final String date;
    private final String category;
    private final String itemName;

    // Constructor matching the parameters from your adapter
    public Review(String user, int rate, String comment, String date, String category, String itemName) {
        this.user = user;
        this.rate = rate;
        this.comment = comment;
        this.date = date;
        this.category = category;
        this.itemName = itemName;
    }

    // Getters
    public String getUser() {
        return user;
    }

    public int getRate() {
        return rate;
    }

    public String getComment() {
        return comment;
    }

    public String getDate() {
        return date;
    }

    public String getCategory() {
        return category;
    }

    public String getItemName() {
        return itemName;
    }
}
