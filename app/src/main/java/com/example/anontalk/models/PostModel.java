package com.example.anontalk.models;

import com.google.firebase.Timestamp;
import java.util.List;

public class PostModel {

    private String postId;
    private String text;
    private List<String> images;
    private Object timestamp; // 🔥 accept both long & Timestamp
    private int likeCount;
    private int commentCount;

    public PostModel() {}

    public PostModel(String postId, String text, List<String> images,
                     Object timestamp, int likeCount, int commentCount) {
        this.postId = postId;
        this.text = text;
        this.images = images;
        this.timestamp = timestamp;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
    }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    // 🔥 SAFE TIMESTAMP CONVERSION
    public long getTimestamp() {
        if (timestamp instanceof Long) {
            return (Long) timestamp;
        } else if (timestamp instanceof Timestamp) {
            return ((Timestamp) timestamp).toDate().getTime();
        } else {
            return 0;
        }
    }

    public void setTimestamp(Object timestamp) {
        this.timestamp = timestamp;
    }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }
}
