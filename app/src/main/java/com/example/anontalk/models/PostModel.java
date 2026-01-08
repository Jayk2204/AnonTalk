package com.example.anontalk.models;

import com.google.firebase.Timestamp;

public class PostModel {

    private String id;   // 🔥 Firestore document ID
    private String userId;
    private String text;
    private String category;
    private long likes;
    private Timestamp timestamp;

    public PostModel() {}

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getText() { return text; }
    public String getCategory() { return category; }
    public long getLikes() { return likes; }
    public Timestamp getTimestamp() { return timestamp; }

    public void setId(String id) { this.id = id; }   // 🔥 REQUIRED
}
