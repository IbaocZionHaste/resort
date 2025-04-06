package com.example.resort.aboutus.data;

public class Question {
    private String title;
    private String answer;
    private long timestamp;

    // Default constructor required for Firebase
    public Question() {}

    public Question(String title, String answer, long timestamp) {
        this.title = title;
        this.answer = answer;
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return title;
    }

    public String getAnswer() {
        return answer;
    }

    public long getTimestamp() {
        return timestamp;
    }
}