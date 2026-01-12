package com.example.anontalk.models;

import com.google.firebase.Timestamp;

import java.util.Date;

public class FeedItem {

    private String type; // "POST" or "POLL"
    private PostModel post;
    private PollModel poll;

    public FeedItem() {}

    public FeedItem(String type, PostModel post, PollModel poll) {
        this.type = type;
        this.post = post;
        this.poll = poll;
    }

    public String getType() {
        return type;
    }

    public PostModel getPost() {
        return post;
    }

    public PollModel getPoll() {
        return poll;
    }

    // 🔥 FIXED: Convert long → Timestamp safely
    public Timestamp getTimestamp() {

        if ("POST".equals(type) && post != null) {
            // Post timestamp is long (milliseconds)
            return new Timestamp(new Date(post.getTimestamp()));
        }

        if ("POLL".equals(type) && poll != null) {
            // Poll already stores Firestore Timestamp
            return poll.getCreatedAt();
        }

        return null;
    }
}
