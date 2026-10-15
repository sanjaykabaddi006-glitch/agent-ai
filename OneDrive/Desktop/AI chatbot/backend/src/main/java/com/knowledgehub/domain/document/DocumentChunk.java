package com.knowledgehub.domain.document;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "document_id", nullable = false, length = 36)
    private String documentId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "token_estimate", nullable = false)
    private int tokenEstimate;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "section_name", length = 255)
    private String sectionName;

    @Column(name = "embedding_model", length = 64)
    private String embeddingModel;

    @Column(name = "embedding_dimensions")
    private Integer embeddingDimensions;

    @Column(name = "embedding_vector_json", columnDefinition = "TEXT")
    private String embeddingVectorJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public DocumentChunk() {
        this.id = UUID.randomUUID().toString();
    }

    public DocumentChunk(String documentId, String userId, int chunkIndex, String content, Integer pageNumber, String sectionName) {
        this.id = UUID.randomUUID().toString();
        this.documentId = documentId;
        this.userId = userId;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.tokenEstimate = content != null ? Math.max(1, content.split("\\s+").length) : 0;
        this.pageNumber = pageNumber;
        this.sectionName = sectionName;
        this.createdAt = Instant.now();
    }

    public List<Double> getVectorList() {
        if (embeddingVectorJson == null || embeddingVectorJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(embeddingVectorJson, new TypeReference<List<Double>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public void setVectorList(List<Double> vector) {
        if (vector == null || vector.isEmpty()) {
            this.embeddingVectorJson = null;
            this.embeddingDimensions = 0;
            return;
        }
        try {
            this.embeddingVectorJson = MAPPER.writeValueAsString(vector);
            this.embeddingDimensions = vector.size();
        } catch (Exception e) {
            this.embeddingVectorJson = null;
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getTokenEstimate() { return tokenEstimate; }
    public void setTokenEstimate(int tokenEstimate) { this.tokenEstimate = tokenEstimate; }

    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }

    public Integer getEmbeddingDimensions() { return embeddingDimensions; }
    public void setEmbeddingDimensions(Integer embeddingDimensions) { this.embeddingDimensions = embeddingDimensions; }

    public String getEmbeddingVectorJson() { return embeddingVectorJson; }
    public void setEmbeddingVectorJson(String embeddingVectorJson) { this.embeddingVectorJson = embeddingVectorJson; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
