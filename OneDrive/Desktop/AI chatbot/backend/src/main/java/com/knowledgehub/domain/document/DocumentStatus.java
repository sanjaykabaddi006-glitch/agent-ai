package com.knowledgehub.domain.document;

public enum DocumentStatus {
    PENDING,
    EXTRACTING,
    CONVERTING_OKF,
    CHUNKING,
    EMBEDDING,
    COMPLETED,
    FAILED
}
