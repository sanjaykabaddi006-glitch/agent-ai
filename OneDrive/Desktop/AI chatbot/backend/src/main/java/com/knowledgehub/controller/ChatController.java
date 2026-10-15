package com.knowledgehub.controller;

import com.knowledgehub.common.ApiResponse;
import com.knowledgehub.dto.chat.ChatMessageResponse;
import com.knowledgehub.dto.chat.ChatSessionResponse;
import com.knowledgehub.dto.chat.CreateSessionRequest;
import com.knowledgehub.dto.chat.SendMessageRequest;
import com.knowledgehub.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.knowledgehub.dto.chat.UpdateMessageRequest;

@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "Chat", description = "Conversational RAG workspace and session management")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/sessions")
    @Operation(summary = "Create a new chat conversation session")
    public ResponseEntity<ApiResponse<ChatSessionResponse>> createSession(
            @RequestBody(required = false) CreateSessionRequest request,
            Authentication authentication) {

        String userId = authentication.getName();
        ChatSessionResponse response = chatService.createSession(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Session created", response));
    }

    @GetMapping("/sessions")
    @Operation(summary = "List all chat sessions for the authenticated user")
    public ResponseEntity<ApiResponse<List<ChatSessionResponse>>> listSessions(Authentication authentication) {
        String userId = authentication.getName();
        List<ChatSessionResponse> response = chatService.listSessions(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/sessions/{id}/messages")
    @Operation(summary = "Get message history for a chat session")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getSessionMessages(
            @PathVariable("id") String id,
            Authentication authentication) {

        String userId = authentication.getName();
        List<ChatMessageResponse> response = chatService.getSessionMessages(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/sessions/{id}/messages")
    @Operation(summary = "Send question, retrieve relevant document knowledge, and receive grounded response")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @PathVariable("id") String id,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        String userId = authentication.getName();
        String ip = httpRequest.getRemoteAddr();
        ChatMessageResponse response = chatService.sendMessage(id, userId, request, ip);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/sessions/{sessionId}/messages/{messageId}")
    @Operation(summary = "Edit message content in a chat session")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> updateMessage(
            @PathVariable("sessionId") String sessionId,
            @PathVariable("messageId") String messageId,
            @Valid @RequestBody UpdateMessageRequest request,
            Authentication authentication) {

        String userId = authentication.getName();
        ChatMessageResponse response = chatService.updateMessage(sessionId, messageId, userId, request.getContent());
        return ResponseEntity.ok(ApiResponse.ok("Message updated", response));
    }

    @DeleteMapping("/sessions/{id}")
    @Operation(summary = "Delete chat session and conversation history")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @PathVariable("id") String id,
            Authentication authentication) {

        String userId = authentication.getName();
        chatService.deleteSession(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Chat session deleted", null));
    }
}
