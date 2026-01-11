package com.example.anontalk.models;

import com.google.firebase.Timestamp;
import java.util.List;

public class PostModel {

    private String postId;
    private String text;
    private List<String> images;
    private Object timestamp;
    private int likeCount;
    private int commentCount;

    public PostModel() {}

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getText() { return text; }
    public List<String> getImages() { return images; }

    public long getTimestamp() {
        if (timestamp instanceof Long) {
            return (Long) timestamp;
        } else if (timestamp instanceof Timestamp) {
            return ((Timestamp) timestamp).toDate().getTime();
        } else {
            return 0;
        }
    }

    public int getLikeCount() { return likeCount; }
    public int getCommentCount() { return commentCount; }
}
