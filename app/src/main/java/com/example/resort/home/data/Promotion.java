package com.example.resort.home.data;

public class Promotion {
    private String title, description, discount, duration, startDate, imageBase64;

    // Required empty constructor for Firebase
    public Promotion() {
    }

    public Promotion(String title, String description, String discount, String duration, String startDate, String imageBase64) {
        this.title = title;
        this.description = description;
        this.discount = discount;
        this.duration = duration;
        this.startDate = startDate;
        this.imageBase64 = imageBase64;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDiscount() {
        return discount;
    }

    public String getDuration() {
        return duration;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getImageBase64() {
        return imageBase64;
    }
}
