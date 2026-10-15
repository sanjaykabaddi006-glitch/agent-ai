package com.knowledgehub.dto.document;

import java.time.Instant;

public class DocumentResponse {
    private String id;
    private String title;
    private String originalFilename;
    private String fileType;
    private Long fileSize;
    private String status;
    private String errorMessage;
    private int chunkCount;
    private int pageCount;
    private Instant createdAt;
    private Instant updatedAt;

    public DocumentResponse() {}

    public DocumentResponse(String id, String title, String originalFilename, String fileType, Long fileSize,
                            String status, String errorMessage, int chunkCount, int pageCount, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.originalFilename = originalFilename;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.status = status;
        this.errorMessage = errorMessage;
        this.chunkCount = chunkCount;
        this.pageCount = pageCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getChunkCount() { return chunkCount; }
    public void setChunkCount(int chunkCount) { this.chunkCount = chunkCount; }

    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
