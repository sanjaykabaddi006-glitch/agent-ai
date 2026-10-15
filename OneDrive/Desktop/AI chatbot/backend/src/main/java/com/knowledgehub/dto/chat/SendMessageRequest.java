package com.knowledgehub.dto.chat;

import jakarta.validation.constraints.NotBlank;

public class SendMessageRequest {
    @NotBlank(message = "Message content is required")
    private String content;

    private String documentId;
    private Integer topK;
    private Double similarityThreshold;
    private Boolean hybridSearch;

    public SendMessageRequest() {}

    public SendMessageRequest(String content) {
        this.content = content;
    }

    public SendMessageRequest(String content, String documentId) {
        this.content = content;
        this.documentId = documentId;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }

    public Double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(Double similarityThreshold) { this.similarityThreshold = similarityThreshold; }

    public Boolean getHybridSearch() { return hybridSearch; }
    public void setHybridSearch(Boolean hybridSearch) { this.hybridSearch = hybridSearch; }
}
