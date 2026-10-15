package com.knowledgehub.service;

import com.knowledgehub.config.KnowledgeHubProperties;
import com.knowledgehub.domain.document.*;
import com.knowledgehub.okf.OkfDocument;
import com.knowledgehub.okf.OkfService;
import com.knowledgehub.okf.OkfValidationResult;
import com.knowledgehub.okf.OkfValidator;
import com.knowledgehub.parser.DocumentParser;
import com.knowledgehub.parser.DocumentParserFactory;
import com.knowledgehub.parser.ParsedDocument;
import com.knowledgehub.rag.OllamaEmbeddingService;
import com.knowledgehub.rag.TextChunk;
import com.knowledgehub.rag.TextChunker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final ProcessingJobRepository jobRepository;
    private final DocumentParserFactory parserFactory;
    private final OkfService okfService;
    private final OkfValidator okfValidator;
    private final TextChunker textChunker;
    private final OllamaEmbeddingService embeddingService;
    private final KnowledgeHubProperties properties;

    public IngestionService(KnowledgeDocumentRepository documentRepository,
                            DocumentChunkRepository chunkRepository,
                            ProcessingJobRepository jobRepository,
                            DocumentParserFactory parserFactory,
                            OkfService okfService,
                            OkfValidator okfValidator,
                            TextChunker textChunker,
                            OllamaEmbeddingService embeddingService,
                            KnowledgeHubProperties properties) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.jobRepository = jobRepository;
        this.parserFactory = parserFactory;
        this.okfService = okfService;
        this.okfValidator = okfValidator;
        this.textChunker = textChunker;
        this.embeddingService = embeddingService;
        this.properties = properties;
    }

    @Async
    public void processDocumentAsync(String documentId) {
        processDocument(documentId);
    }

    public void processDocument(String documentId) {
        // Resilient lookup with short retry in case of commit timing delay
        KnowledgeDocument doc = null;
        for (int i = 0; i < 5; i++) {
            doc = documentRepository.findById(documentId).orElse(null);
            if (doc != null) break;
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
        }

        if (doc == null) {
            log.error("Ingestion failed: document not found after retries: {}", documentId);
            return;
        }

        // Clean previous chunks if retrying to prevent duplicates
        chunkRepository.deleteByDocumentId(documentId);

        ProcessingJob job = new ProcessingJob(documentId, JobStatus.RUNNING, "STARTING");
        job = jobRepository.saveAndFlush(job);

        try {
            Path uploadDirPath = Paths.get(properties.getStorage().getUploadDir()).toAbsolutePath().normalize();
            File file = uploadDirPath.resolve(doc.getStoredFilename()).toFile();
            if (!file.exists()) {
                throw new IllegalStateException("Stored file not found on disk: " + doc.getStoredFilename());
            }

            // Stage 1: Extraction
            updateStage(doc, job, DocumentStatus.EXTRACTING, "Extracting document content and metadata");
            DocumentParser parser = parserFactory.getParser(doc.getOriginalFilename());
            ParsedDocument parsedDoc;
            try (InputStream is = new FileInputStream(file)) {
                parsedDoc = parser.parse(is, doc.getOriginalFilename());
            }

            if (parsedDoc.getFullText() == null || parsedDoc.getFullText().trim().isEmpty()) {
                throw new IllegalStateException("Parsed document contains no extractable text");
            }

            if (parsedDoc.getTitle() != null && !parsedDoc.getTitle().isBlank()) {
                doc.setTitle(parsedDoc.getTitle());
            }
            doc.setPageCount(parsedDoc.getPageCount());

            // Stage 2: OKF v0.2 Generation & Validation
            updateStage(doc, job, DocumentStatus.CONVERTING_OKF, "Generating Open Knowledge Format v0.2 representation");
            OkfDocument okfDoc = okfService.generateFromParsed(doc, parsedDoc);
            OkfValidationResult valResult = okfValidator.validate(okfDoc);
            if (!valResult.isValid()) {
                log.warn("OKF validation warnings/errors: {}", String.join("; ", valResult.getErrors()));
            }

            String okfFilePath = okfService.saveOkfFile(doc.getId(), okfDoc.getRawContent());
            doc.setOkfPath(okfFilePath);

            // Stage 3: Chunking
            updateStage(doc, job, DocumentStatus.CHUNKING, "Splitting document into contextual chunks");
            int chunkSize = properties.getRag().getChunkSize();
            int chunkOverlap = properties.getRag().getChunkOverlap();
            List<TextChunk> rawChunks = textChunker.chunkDocument(parsedDoc, chunkSize, chunkOverlap);

            if (rawChunks.isEmpty()) {
                throw new IllegalStateException("No valid chunks were generated from the document content");
            }

            // Stage 4: Embedding generation & vector persistence (Fast batching & parallel embeddings)
            updateStage(doc, job, DocumentStatus.EMBEDDING, "Generating vector embeddings for " + rawChunks.size() + " chunks");
            int dimensions = embeddingService.getEmbeddingDimensions();
            String embeddingModel = properties.getOllama().getEmbeddingModel();

            List<String> chunkContents = rawChunks.stream().map(TextChunk::getContent).toList();
            List<List<Double>> embeddings = embeddingService.generateBatchEmbeddings(chunkContents);

            List<DocumentChunk> entityChunks = new ArrayList<>(rawChunks.size());
            for (int i = 0; i < rawChunks.size(); i++) {
                TextChunk tc = rawChunks.get(i);
                DocumentChunk chunkEntity = new DocumentChunk(
                        doc.getId(),
                        doc.getUserId(),
                        tc.getIndex(),
                        tc.getContent(),
                        tc.getPageNumber(),
                        tc.getSectionName()
                );
                chunkEntity.setEmbeddingModel(embeddingModel);
                chunkEntity.setEmbeddingDimensions(dimensions);

                List<Double> vector = (i < embeddings.size()) ? embeddings.get(i) : Collections.emptyList();
                chunkEntity.setVectorList(vector);

                entityChunks.add(chunkEntity);
            }

            chunkRepository.saveAllAndFlush(entityChunks);

            // Mark completed
            doc.setStatus(DocumentStatus.COMPLETED);
            doc.setChunkCount(entityChunks.size());
            doc.setErrorMessage(null);
            doc.setUpdatedAt(Instant.now());
            documentRepository.saveAndFlush(doc);

            job.setStatus(JobStatus.COMPLETED);
            job.setStage("COMPLETED");
            job.setCompletedAt(Instant.now());
            jobRepository.saveAndFlush(job);

            log.info("Successfully ingested document '{}' (id: {}) with {} chunks into vector index",
                    doc.getTitle(), doc.getId(), entityChunks.size());

        } catch (Exception e) {
            log.error("Failed ingestion for document {}: {}", documentId, e.getMessage(), e);

            doc.setStatus(DocumentStatus.FAILED);
            doc.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Document processing failed");
            doc.setUpdatedAt(Instant.now());
            documentRepository.saveAndFlush(doc);

            if (job != null) {
                job.setStatus(JobStatus.FAILED);
                job.setStage("FAILED");
                job.setErrorMessage(doc.getErrorMessage());
                job.setCompletedAt(Instant.now());
                jobRepository.saveAndFlush(job);
            }
        }
    }

    private void updateStage(KnowledgeDocument doc, ProcessingJob job, DocumentStatus status, String stageDescription) {
        doc.setStatus(status);
        doc.setUpdatedAt(Instant.now());
        documentRepository.saveAndFlush(doc);

        job.setStage(stageDescription);
        jobRepository.saveAndFlush(job);
    }
}
