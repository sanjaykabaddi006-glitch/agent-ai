package com.knowledgehub.dto.chat;

import jakarta.validation.constraints.NotBlank;

public class UpdateMessageRequest {
    @NotBlank(message = "Message content is required")
    private String content;

    public UpdateMessageRequest() {}

    public UpdateMessageRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
