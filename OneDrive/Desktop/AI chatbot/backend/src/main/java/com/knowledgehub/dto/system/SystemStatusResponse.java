package com.knowledgehub.dto.system;

import java.util.List;

public class SystemStatusResponse {
    private String status = "UP";
    private boolean databaseConnected;
    private boolean ollamaConnected;
    private String ollamaBaseUrl;
    private String configuredChatModel;
    private String configuredEmbeddingModel;
    private int embeddingDimensions;
    private List<String> availableOllamaModels;
    private long totalDocuments;
    private long indexedDocuments;
    private long processingDocuments;
    private long failedDocuments;
    private long totalChunks;
    private long totalChatSessions;

    public SystemStatusResponse() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isDatabaseConnected() { return databaseConnected; }
    public void setDatabaseConnected(boolean databaseConnected) { this.databaseConnected = databaseConnected; }

    public boolean isOllamaConnected() { return ollamaConnected; }
    public void setOllamaConnected(boolean ollamaConnected) { this.ollamaConnected = ollamaConnected; }

    public String getOllamaBaseUrl() { return ollamaBaseUrl; }
    public void setOllamaBaseUrl(String ollamaBaseUrl) { this.ollamaBaseUrl = ollamaBaseUrl; }

    public String getConfiguredChatModel() { return configuredChatModel; }
    public void setConfiguredChatModel(String configuredChatModel) { this.configuredChatModel = configuredChatModel; }

    public String getConfiguredEmbeddingModel() { return configuredEmbeddingModel; }
    public void setConfiguredEmbeddingModel(String configuredEmbeddingModel) { this.configuredEmbeddingModel = configuredEmbeddingModel; }

    public int getEmbeddingDimensions() { return embeddingDimensions; }
    public void setEmbeddingDimensions(int embeddingDimensions) { this.embeddingDimensions = embeddingDimensions; }

    public List<String> getAvailableOllamaModels() { return availableOllamaModels; }
    public void setAvailableOllamaModels(List<String> availableOllamaModels) { this.availableOllamaModels = availableOllamaModels; }

    public long getTotalDocuments() { return totalDocuments; }
    public void setTotalDocuments(long totalDocuments) { this.totalDocuments = totalDocuments; }

    public long getIndexedDocuments() { return indexedDocuments; }
    public void setIndexedDocuments(long indexedDocuments) { this.indexedDocuments = indexedDocuments; }

    public long getProcessingDocuments() { return processingDocuments; }
    public void setProcessingDocuments(long processingDocuments) { this.processingDocuments = processingDocuments; }

    public long getFailedDocuments() { return failedDocuments; }
    public void setFailedDocuments(long failedDocuments) { this.failedDocuments = failedDocuments; }

    public long getTotalChunks() { return totalChunks; }
    public void setTotalChunks(long totalChunks) { this.totalChunks = totalChunks; }

    public long getTotalChatSessions() { return totalChatSessions; }
    public void setTotalChatSessions(long totalChatSessions) { this.totalChatSessions = totalChatSessions; }
}
