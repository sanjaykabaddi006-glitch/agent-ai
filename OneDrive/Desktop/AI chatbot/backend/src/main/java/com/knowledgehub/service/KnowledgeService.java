package com.knowledgehub.service;

import com.knowledgehub.domain.document.KnowledgeDocument;
import com.knowledgehub.domain.document.KnowledgeDocumentRepository;
import com.knowledgehub.dto.knowledge.ConceptResponse;
import com.knowledgehub.okf.OkfDocument;
import com.knowledgehub.okf.OkfService;
import com.knowledgehub.rag.OllamaEmbeddingService;
import com.knowledgehub.rag.RetrievalResult;
import com.knowledgehub.rag.VectorSearchService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeService {

    private final KnowledgeDocumentRepository documentRepository;
    private final OkfService okfService;
    private final VectorSearchService vectorSearchService;
    private final OllamaEmbeddingService embeddingService;

    public KnowledgeService(KnowledgeDocumentRepository documentRepository,
                            OkfService okfService,
                            VectorSearchService vectorSearchService,
                            OllamaEmbeddingService embeddingService) {
        this.documentRepository = documentRepository;
        this.okfService = okfService;
        this.vectorSearchService = vectorSearchService;
        this.embeddingService = embeddingService;
    }

    public List<RetrievalResult> searchKnowledge(String query, String userId, Integer topK, Double threshold, Boolean hybrid) {
        List<Double> queryVector = embeddingService.generateEmbedding(query);
        return vectorSearchService.search(queryVector, query, userId, topK, threshold, hybrid);
    }

    public List<ConceptResponse> listConcepts(String userId) {
        List<KnowledgeDocument> docs = documentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<ConceptResponse> concepts = new ArrayList<>();

        for (KnowledgeDocument doc : docs) {
            if (doc.getOkfPath() != null) {
                try {
                    OkfDocument okf = okfService.readOkfDocument(doc.getId(), userId);
                    concepts.add(new ConceptResponse(
                            doc.getId(),
                            okf.getFrontmatter().getType(),
                            okf.getFrontmatter().getTitle(),
                            okf.getFrontmatter().getDescription(),
                            okf.getFrontmatter().getTags(),
                            okf.getFrontmatter().getSources(),
                            okf.getRawContent()
                    ));
                } catch (Exception ignored) {
                }
            }
        }
        return concepts;
    }

    public ConceptResponse getConcept(String docId, String userId) {
        OkfDocument okf = okfService.readOkfDocument(docId, userId);
        return new ConceptResponse(
                docId,
                okf.getFrontmatter().getType(),
                okf.getFrontmatter().getTitle(),
                okf.getFrontmatter().getDescription(),
                okf.getFrontmatter().getTags(),
                okf.getFrontmatter().getSources(),
                okf.getRawContent()
        );
    }

    public byte[] exportBundleZip(String userId) throws IOException {
        return okfService.exportBundleZip(userId);
    }
}
