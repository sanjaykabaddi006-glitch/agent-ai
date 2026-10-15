package com.knowledgehub.dto.chat;

import jakarta.validation.constraints.NotBlank;

public class CreateSessionRequest {
    @NotBlank(message = "Title is required")
    private String title = "New Conversation";

    public CreateSessionRequest() {}

    public CreateSessionRequest(String title) {
        this.title = title;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
