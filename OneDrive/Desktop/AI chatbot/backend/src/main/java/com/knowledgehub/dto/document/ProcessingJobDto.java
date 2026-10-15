package com.knowledgehub.dto.document;

import java.time.Instant;

public class ProcessingJobDto {
    private String id;
    private String status;
    private String stage;
    private String errorMessage;
    private Instant startedAt;
    private Instant completedAt;

    public ProcessingJobDto() {}

    public ProcessingJobDto(String id, String status, String stage, String errorMessage, Instant startedAt, Instant completedAt) {
        this.id = id;
        this.status = status;
        this.stage = stage;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
