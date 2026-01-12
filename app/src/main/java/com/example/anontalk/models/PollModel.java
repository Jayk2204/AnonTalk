package com.example.anontalk.models;

import com.google.firebase.Timestamp;

public class PollModel {

    private String pollId;
    private String question;
    private String createdBy;
    private Timestamp createdAt;
    private Timestamp expiresAt;
    private long totalVotes;

    public PollModel() {}

    public String getPollId() { return pollId; }
    public void setPollId(String pollId) { this.pollId = pollId; }

    public String getQuestion() { return question; }
    public String getCreatedBy() { return createdBy; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getExpiresAt() { return expiresAt; }
    public long getTotalVotes() { return totalVotes; }
}
