package com.knowledgehub.controller;

import com.knowledgehub.common.ApiResponse;
import com.knowledgehub.common.PageResponse;
import com.knowledgehub.dto.document.DocumentDetailResponse;
import com.knowledgehub.dto.document.DocumentResponse;
import com.knowledgehub.okf.OkfDocument;
import com.knowledgehub.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documents", description = "Document ingestion, listing, status, and lifecycle management")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and ingest a document")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            Authentication authentication,
            HttpServletRequest request) {

        String userId = authentication.getName();
        String ip = request.getRemoteAddr();
        DocumentResponse response = documentService.uploadDocument(file, userId, ip);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Document uploaded and ingestion started", response));
    }

    @GetMapping
    @Operation(summary = "List documents with pagination and optional search")
    public ResponseEntity<ApiResponse<PageResponse<DocumentResponse>>> listDocuments(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Authentication authentication) {

        String userId = authentication.getName();
        PageResponse<DocumentResponse> response = documentService.listDocuments(userId, search, page, size);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document details and processing job logs")
    public ResponseEntity<ApiResponse<DocumentDetailResponse>> getDocument(
            @PathVariable("id") String id,
            Authentication authentication) {

        String userId = authentication.getName();
        DocumentDetailResponse response = documentService.getDocument(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Get current processing status of a document")
    public ResponseEntity<ApiResponse<DocumentDetailResponse>> getStatus(
            @PathVariable("id") String id,
            Authentication authentication) {

        String userId = authentication.getName();
        DocumentDetailResponse response = documentService.getDocument(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry failed document ingestion")
    public ResponseEntity<ApiResponse<DocumentResponse>> retryDocument(
            @PathVariable("id") String id,
            Authentication authentication,
            HttpServletRequest request) {

        String userId = authentication.getName();
        String ip = request.getRemoteAddr();
        DocumentResponse response = documentService.retryDocument(id, userId, ip);
        return ResponseEntity.ok(ApiResponse.ok("Ingestion retry initiated", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete document and its indexed vector chunks")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable("id") String id,
            Authentication authentication,
            HttpServletRequest request) {

        String userId = authentication.getName();
        String ip = request.getRemoteAddr();
        documentService.deleteDocument(id, userId, ip);
        return ResponseEntity.ok(ApiResponse.ok("Document deleted successfully", null));
    }

    @GetMapping("/{id}/okf")
    @Operation(summary = "Get Open Knowledge Format (OKF v0.2) Markdown representation")
    public ResponseEntity<ApiResponse<OkfDocument>> getOkf(
            @PathVariable("id") String id,
            Authentication authentication) {

        String userId = authentication.getName();
        OkfDocument response = documentService.getOkfRepresentation(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
