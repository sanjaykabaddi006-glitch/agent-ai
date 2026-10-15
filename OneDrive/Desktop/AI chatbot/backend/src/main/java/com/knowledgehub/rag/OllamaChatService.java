package com.knowledgehub.rag;

import java.util.List;

public interface OllamaChatService {
    default String generateAnswer(String systemPrompt, String userPrompt, List<ChatMessageDto> history) {
        return generateAnswer(systemPrompt, userPrompt, history, 120);
    }

    String generateAnswer(String systemPrompt, String userPrompt, List<ChatMessageDto> history, Integer numPredict);

    record ChatMessageDto(String role, String content) {}
}
