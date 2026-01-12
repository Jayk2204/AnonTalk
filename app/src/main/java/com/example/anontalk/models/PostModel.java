package com.example.anontalk.models;

import com.google.firebase.Timestamp;
import java.util.List;

public class PostModel {

    private String postId;
    private String userId;          // 🔥 ADD THIS
    private String text;
    private List<String> images;
    private Object timestamp;
    private int likeCount;
    private int commentCount;

    public PostModel() {}

    // 🔑 Post ID (document id)
    public String getPostId() {
        return postId;
    }
    public void setPostId(String postId) {
        this.postId = postId;
    }

    // 👤 Owner ID
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    // 📝 Post text
    public String getText() {
        return text;
    }

    // 🖼️ Image URLs (IMGBB)
    public List<String> getImages() {
        return images;
    }

    // ⏱ Timestamp (Long or Firebase Timestamp safe)
    public long getTimestamp() {
        if (timestamp instanceof Long) {
            return (Long) timestamp;
        } else if (timestamp instanceof Timestamp) {
            return ((Timestamp) timestamp).toDate().getTime();
        } else {
            return 0;
        }
    }

    // ❤️ Likes
    public int getLikeCount() {
        return likeCount;
    }

    // 💬 Comments
    public int getCommentCount() {
        return commentCount;
    }
}
