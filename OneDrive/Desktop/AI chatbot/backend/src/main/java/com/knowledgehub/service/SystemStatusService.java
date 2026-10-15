package com.knowledgehub.service;

import com.knowledgehub.config.KnowledgeHubProperties;
import com.knowledgehub.domain.chat.ChatSessionRepository;
import com.knowledgehub.domain.document.DocumentChunkRepository;
import com.knowledgehub.domain.document.DocumentStatus;
import com.knowledgehub.domain.document.KnowledgeDocumentRepository;
import com.knowledgehub.dto.system.SystemStatusResponse;
import com.knowledgehub.rag.OllamaClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SystemStatusService {

    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final ChatSessionRepository sessionRepository;
    private final OllamaClient ollamaClient;
    private final KnowledgeHubProperties properties;

    public SystemStatusService(KnowledgeDocumentRepository documentRepository,
                               DocumentChunkRepository chunkRepository,
                               ChatSessionRepository sessionRepository,
                               OllamaClient ollamaClient,
                               KnowledgeHubProperties properties) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.sessionRepository = sessionRepository;
        this.ollamaClient = ollamaClient;
        this.properties = properties;
    }

    public SystemStatusResponse getSystemStatus(String userId) {
        SystemStatusResponse resp = new SystemStatusResponse();
        resp.setStatus("UP");
        resp.setDatabaseConnected(true);

        boolean ollamaOk = ollamaClient.isConnected();
        resp.setOllamaConnected(ollamaOk);
        resp.setOllamaBaseUrl(properties.getOllama().getBaseUrl());
        resp.setConfiguredChatModel(properties.getOllama().getChatModel());
        resp.setConfiguredEmbeddingModel(properties.getOllama().getEmbeddingModel());
        resp.setEmbeddingDimensions(ollamaClient.getEmbeddingDimensions());
        resp.setAvailableOllamaModels(ollamaClient.getAvailableModels());

        if (userId != null) {
            resp.setTotalDocuments(documentRepository.countByUserId(userId));
            resp.setIndexedDocuments(documentRepository.countByUserIdAndStatus(userId, DocumentStatus.COMPLETED));
            resp.setProcessingDocuments(documentRepository.countByUserIdAndStatus(userId, DocumentStatus.EXTRACTING)
                    + documentRepository.countByUserIdAndStatus(userId, DocumentStatus.CONVERTING_OKF)
                    + documentRepository.countByUserIdAndStatus(userId, DocumentStatus.CHUNKING)
                    + documentRepository.countByUserIdAndStatus(userId, DocumentStatus.EMBEDDING));
            resp.setFailedDocuments(documentRepository.countByUserIdAndStatus(userId, DocumentStatus.FAILED));
            resp.setTotalChunks(chunkRepository.findByUserId(userId).size());
            resp.setTotalChatSessions(sessionRepository.countByUserId(userId));
        } else {
            resp.setTotalDocuments(documentRepository.count());
            resp.setIndexedDocuments(0);
            resp.setProcessingDocuments(0);
            resp.setFailedDocuments(0);
            resp.setTotalChunks(chunkRepository.count());
            resp.setTotalChatSessions(sessionRepository.count());
        }

        return resp;
    }

    public List<String> getAvailableModels() {
        return ollamaClient.getAvailableModels();
    }
}
