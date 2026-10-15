package com.knowledgehub.service;

import com.knowledgehub.common.exception.ResourceNotFoundException;
import com.knowledgehub.domain.chat.*;
import com.knowledgehub.dto.chat.*;
import com.knowledgehub.rag.Citation;
import com.knowledgehub.rag.OllamaChatService;
import com.knowledgehub.rag.RagResponse;
import com.knowledgehub.rag.RagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final RagService ragService;
    private final AuditService auditService;

    public ChatService(ChatSessionRepository sessionRepository,
                       ChatMessageRepository messageRepository,
                       RagService ragService,
                       AuditService auditService) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.ragService = ragService;
        this.auditService = auditService;
    }

    @Transactional
    public ChatSessionResponse createSession(String userId, CreateSessionRequest request) {
        String title = (request != null && request.getTitle() != null && !request.getTitle().isBlank())
                ? request.getTitle().trim() : "New Chat Session";

        ChatSession session = new ChatSession(userId, title);
        ChatSession saved = sessionRepository.save(session);

        return new ChatSessionResponse(saved.getId(), saved.getTitle(), 0, saved.getCreatedAt(), saved.getUpdatedAt());
    }

    public List<ChatSessionResponse> listSessions(String userId) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(s -> new ChatSessionResponse(
                        s.getId(),
                        s.getTitle(),
                        messageRepository.countBySessionId(s.getId()),
                        s.getCreatedAt(),
                        s.getUpdatedAt()
                ))
                .collect(Collectors.toList());
    }

    public List<ChatMessageResponse> getSessionMessages(String sessionId, String userId) {
        sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found: " + sessionId));

        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(m -> new ChatMessageResponse(
                        m.getId(),
                        m.getSessionId(),
                        m.getRole().name(),
                        m.getContent(),
                        m.getCitations(),
                        m.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageResponse sendMessage(String sessionId, String userId, SendMessageRequest request, String ipAddress) {
        ChatSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found: " + sessionId));

        // 1. Save User Message
        ChatMessage userMessage = new ChatMessage(sessionId, MessageRole.USER, request.getContent().trim());
        messageRepository.save(userMessage);

        // If default title, update session title based on first query
        if ("New Chat Session".equalsIgnoreCase(session.getTitle()) || "New Conversation".equalsIgnoreCase(session.getTitle())) {
            String smartTitle = request.getContent().trim();
            if (smartTitle.length() > 40) {
                smartTitle = smartTitle.substring(0, 37) + "...";
            }
            session.setTitle(smartTitle);
        }
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);

        // 2. Fetch past conversation context (preserve history for conversational accuracy and continuity)
        List<ChatMessage> existingMessages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        List<OllamaChatService.ChatMessageDto> history = new ArrayList<>();
        int startIndex = Math.max(0, existingMessages.size() - 9);
        for (int i = startIndex; i < existingMessages.size() - 1; i++) {
            ChatMessage m = existingMessages.get(i);
            String content = m.getContent();
            if (content != null && content.length() > 800) {
                content = content.substring(0, 797) + "...";
            }
            history.add(new OllamaChatService.ChatMessageDto(m.getRole().name(), content != null ? content : ""));
        }

        // Link document attachment if provided in request
        if (request.getDocumentId() != null && !request.getDocumentId().isBlank()) {
            session.setDocumentId(request.getDocumentId().trim());
        }
        String effectiveDocId = (request.getDocumentId() != null && !request.getDocumentId().isBlank())
                ? request.getDocumentId().trim()
                : session.getDocumentId();

        // 3. Query RAG engine scoped to attached document if present
        RagResponse ragResponse = ragService.query(
                request.getContent(),
                userId,
                effectiveDocId,
                history,
                request.getTopK(),
                request.getSimilarityThreshold(),
                request.getHybridSearch()
        );

        // 4. Save Assistant Response with structured citations
        ChatMessage assistantMessage = new ChatMessage(sessionId, MessageRole.ASSISTANT, ragResponse.getAnswer());
        assistantMessage.setCitations(ragResponse.getCitations());
        ChatMessage savedAssistantMsg = messageRepository.save(assistantMessage);

        auditService.log(userId, "CHAT", "SESSION", sessionId, "Query submitted (grounded: " + ragResponse.isGrounded() + ")", ipAddress);

        return new ChatMessageResponse(
                savedAssistantMsg.getId(),
                savedAssistantMsg.getSessionId(),
                savedAssistantMsg.getRole().name(),
                savedAssistantMsg.getContent(),
                savedAssistantMsg.getCitations(),
                savedAssistantMsg.getCreatedAt()
        );
    }

    @Transactional
    public void deleteSession(String sessionId, String userId) {
        ChatSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found: " + sessionId));

        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.delete(session);
    }

    @Transactional
    public ChatMessageResponse updateMessage(String sessionId, String messageId, String userId, String newContent) {
        sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found: " + sessionId));

        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));

        if (!message.getSessionId().equals(sessionId)) {
            throw new IllegalArgumentException("Message does not belong to session: " + sessionId);
        }

        message.setContent(newContent != null ? newContent.trim() : "");
        ChatMessage saved = messageRepository.save(message);

        return new ChatMessageResponse(
                saved.getId(),
                saved.getSessionId(),
                saved.getRole().name(),
                saved.getContent(),
                saved.getCitations(),
                saved.getCreatedAt()
        );
    }
}
