package com.knowledgehub.dto.chat;

import java.time.Instant;

public class ChatSessionResponse {
    private String id;
    private String title;
    private long messageCount;
    private Instant createdAt;
    private Instant updatedAt;

    public ChatSessionResponse() {}

    public ChatSessionResponse(String id, String title, long messageCount, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.messageCount = messageCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public long getMessageCount() { return messageCount; }
    public void setMessageCount(long messageCount) { this.messageCount = messageCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
