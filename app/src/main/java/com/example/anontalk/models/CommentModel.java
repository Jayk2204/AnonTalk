package com.example.anontalk.models;

public class CommentModel {

    private String commentId;
    private String text;
    private String userId;
    private Object timestamp; // Timestamp or Long
    private boolean edited;


    public CommentModel() {}

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getText() { return text; }
    public String getUserId() { return userId; }

    public boolean isEdited() { return edited; }
    public void setEdited(boolean edited) { this.edited = edited; }

    public long getTimestamp() {
        if (timestamp instanceof Long) {
            return (Long) timestamp;
        } else if (timestamp instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) timestamp).toDate().getTime();
        } else {
            return 0;
        }
    }
}
