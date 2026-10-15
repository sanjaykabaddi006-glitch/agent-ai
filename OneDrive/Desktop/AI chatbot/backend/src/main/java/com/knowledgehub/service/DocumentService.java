package com.knowledgehub.service;

import com.knowledgehub.common.PageResponse;
import com.knowledgehub.common.exception.BadRequestException;
import com.knowledgehub.common.exception.ResourceNotFoundException;
import com.knowledgehub.config.KnowledgeHubProperties;
import com.knowledgehub.domain.document.*;
import com.knowledgehub.dto.document.DocumentDetailResponse;
import com.knowledgehub.dto.document.DocumentResponse;
import com.knowledgehub.dto.document.ProcessingJobDto;
import com.knowledgehub.okf.OkfDocument;
import com.knowledgehub.okf.OkfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final ProcessingJobRepository jobRepository;
    private final IngestionService ingestionService;
    private final OkfService okfService;
    private final AuditService auditService;
    private final KnowledgeHubProperties properties;

    public DocumentService(KnowledgeDocumentRepository documentRepository,
                           DocumentChunkRepository chunkRepository,
                           ProcessingJobRepository jobRepository,
                           IngestionService ingestionService,
                           OkfService okfService,
                           AuditService auditService,
                           KnowledgeHubProperties properties) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.jobRepository = jobRepository;
        this.ingestionService = ingestionService;
        this.okfService = okfService;
        this.auditService = auditService;
        this.properties = properties;
    }

    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, String userId, String ipAddress) {
        if (file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }

        if (file.getSize() > properties.getStorage().getMaxFileSizeBytes()) {
            throw new BadRequestException("File exceeds maximum allowed size of 25MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BadRequestException("Uploaded file must have a valid extension");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!properties.getStorage().getAllowedExtensions().contains(extension)) {
            throw new BadRequestException("Unsupported file type '." + extension +
                    "'. Allowed types: " + String.join(", ", properties.getStorage().getAllowedExtensions()));
        }

        // Store with safe UUID filename to prevent path traversal attacks
        String storedFilename = UUID.randomUUID() + "." + extension;
        Path uploadDirPath = Paths.get(properties.getStorage().getUploadDir()).toAbsolutePath().normalize();
        Path targetPath = uploadDirPath.resolve(storedFilename).normalize();

        try {
            Files.createDirectories(uploadDirPath);
            Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store uploaded file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store uploaded file securely: " + e.getMessage(), e);
        }

        String title = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
        KnowledgeDocument doc = new KnowledgeDocument(
                userId,
                title,
                originalFilename,
                storedFilename,
                extension,
                file.getSize()
        );

        KnowledgeDocument saved = documentRepository.save(doc);
        auditService.log(userId, "UPLOAD", "DOCUMENT", saved.getId(), "Uploaded " + originalFilename, ipAddress);

        // Ingest immediately so all chunks and embeddings are indexed and available right away
        try {
            ingestionService.processDocument(saved.getId());
            // Refresh document entity with updated status and chunk count
            saved = documentRepository.findById(saved.getId()).orElse(saved);
        } catch (Exception e) {
            log.warn("Synchronous ingestion encountered exception, scheduling async fallback: {}", e.getMessage());
            ingestionService.processDocumentAsync(saved.getId());
        }

        return toDocumentResponse(saved);
    }

    public PageResponse<DocumentResponse> listDocuments(String userId, String search, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<KnowledgeDocument> docPage;

        if (search != null && !search.isBlank()) {
            docPage = documentRepository.findByUserIdAndSearch(userId, search.trim(), pageRequest);
        } else {
            docPage = documentRepository.findByUserId(userId, pageRequest);
        }

        List<DocumentResponse> items = docPage.getContent().stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(items, docPage.getNumber(), docPage.getSize(), docPage.getTotalElements(), docPage.getTotalPages());
    }

    public DocumentDetailResponse getDocument(String id, String userId) {
        KnowledgeDocument doc = documentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));

        List<ProcessingJob> jobs = jobRepository.findByDocumentIdOrderByStartedAtDesc(id);
        List<ProcessingJobDto> jobDtos = jobs.stream()
                .map(j -> new ProcessingJobDto(j.getId(), j.getStatus().name(), j.getStage(), j.getErrorMessage(), j.getStartedAt(), j.getCompletedAt()))
                .collect(Collectors.toList());

        DocumentDetailResponse resp = new DocumentDetailResponse();
        resp.setId(doc.getId());
        resp.setTitle(doc.getTitle());
        resp.setOriginalFilename(doc.getOriginalFilename());
        resp.setFileType(doc.getFileType());
        resp.setFileSize(doc.getFileSize());
        resp.setStatus(doc.getStatus().name());
        resp.setErrorMessage(doc.getErrorMessage());
        resp.setOkfPath(doc.getOkfPath());
        resp.setChunkCount(doc.getChunkCount());
        resp.setPageCount(doc.getPageCount());
        resp.setJobs(jobDtos);
        resp.setCreatedAt(doc.getCreatedAt());
        resp.setUpdatedAt(doc.getUpdatedAt());

        return resp;
    }

    @Transactional
    public DocumentResponse retryDocument(String id, String userId, String ipAddress) {
        KnowledgeDocument doc = documentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));

        doc.setStatus(DocumentStatus.PENDING);
        doc.setErrorMessage(null);
        documentRepository.save(doc);

        auditService.log(userId, "RETRY", "DOCUMENT", doc.getId(), "Triggered ingestion retry", ipAddress);

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ingestionService.processDocumentAsync(doc.getId());
                }
            });
        } else {
            ingestionService.processDocumentAsync(doc.getId());
        }

        return toDocumentResponse(doc);
    }

    @Transactional
    public void deleteDocument(String id, String userId, String ipAddress) {
        KnowledgeDocument doc = documentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + id));

        // Delete DB records
        chunkRepository.deleteByDocumentId(id);
        jobRepository.deleteByDocumentId(id);
        documentRepository.delete(doc);

        // Delete physical files
        try {
            Path filePath = Paths.get(properties.getStorage().getUploadDir()).toAbsolutePath().normalize().resolve(doc.getStoredFilename());
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            log.warn("Failed to delete stored file {}: {}", doc.getStoredFilename(), e.getMessage());
        }

        okfService.deleteOkfFile(doc.getOkfPath());

        auditService.log(userId, "DELETE", "DOCUMENT", id, "Deleted document " + doc.getTitle(), ipAddress);
    }

    public OkfDocument getOkfRepresentation(String id, String userId) {
        return okfService.readOkfDocument(id, userId);
    }

    private DocumentResponse toDocumentResponse(KnowledgeDocument doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getTitle(),
                doc.getOriginalFilename(),
                doc.getFileType(),
                doc.getFileSize(),
                doc.getStatus().name(),
                doc.getErrorMessage(),
                doc.getChunkCount(),
                doc.getPageCount(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
