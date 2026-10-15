package com.knowledgehub.domain.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledgehub.rag.Citation;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "citations_json", columnDefinition = "TEXT")
    private String citationsJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ChatMessage() {
        this.id = UUID.randomUUID().toString();
    }

    public ChatMessage(String sessionId, MessageRole role, String content) {
        this.id = UUID.randomUUID().toString();
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public List<Citation> getCitations() {
        if (citationsJson == null || citationsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(citationsJson, new TypeReference<List<Citation>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public void setCitations(List<Citation> citations) {
        if (citations == null || citations.isEmpty()) {
            this.citationsJson = null;
            return;
        }
        try {
            this.citationsJson = MAPPER.writeValueAsString(citations);
        } catch (Exception e) {
            this.citationsJson = null;
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public MessageRole getRole() { return role; }
    public void setRole(MessageRole role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCitationsJson() { return citationsJson; }
    public void setCitationsJson(String citationsJson) { this.citationsJson = citationsJson; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
