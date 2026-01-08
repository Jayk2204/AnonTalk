package com.example.anontalk.models;

public class CommentModel {
    private String text;
    private String userId;
    private long timestamp;

    public CommentModel() {}

    public String getText() { return text; }
    public String getUserId() { return userId; }
    public long getTimestamp() { return timestamp; }
}
