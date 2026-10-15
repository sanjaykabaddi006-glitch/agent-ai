package com.knowledgehub.dto.chat;

import com.knowledgehub.rag.Citation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ChatMessageResponse {
    private String id;
    private String sessionId;
    private String role;
    private String content;
    private List<Citation> citations = new ArrayList<>();
    private Instant createdAt;

    public ChatMessageResponse() {}

    public ChatMessageResponse(String id, String sessionId, String role, String content, List<Citation> citations, Instant createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.citations = citations != null ? citations : new ArrayList<>();
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<Citation> getCitations() { return citations; }
    public void setCitations(List<Citation> citations) { this.citations = citations; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
