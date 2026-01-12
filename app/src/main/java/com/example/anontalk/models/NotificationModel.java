package com.example.anontalk.models;

public class NotificationModel {

    private String type;
    private String fromUserId;
    private String postId;
    private String text;
    private long timestamp;
    private boolean seen;

    public NotificationModel() {}

    public String getType() { return type; }
    public String getFromUserId() { return fromUserId; }
    public String getPostId() { return postId; }
    public String getText() { return text; }
    public long getTimestamp() { return timestamp; }
    public boolean isSeen() { return seen; }
}
