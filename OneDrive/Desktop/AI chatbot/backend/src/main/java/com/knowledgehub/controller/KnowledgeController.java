package com.knowledgehub.controller;

import com.knowledgehub.common.ApiResponse;
import com.knowledgehub.dto.knowledge.ConceptResponse;
import com.knowledgehub.rag.RetrievalResult;
import com.knowledgehub.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge")
@Tag(name = "Knowledge", description = "OKF concepts, vector search, and knowledge bundle export")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping("/search")
    @Operation(summary = "Perform vector and hybrid search across ingested documents")
    public ResponseEntity<ApiResponse<List<RetrievalResult>>> search(
            @RequestParam("query") String query,
            @RequestParam(value = "topK", required = false) Integer topK,
            @RequestParam(value = "threshold", required = false) Double threshold,
            @RequestParam(value = "hybrid", required = false) Boolean hybrid,
            Authentication authentication) {

        String userId = authentication.getName();
        List<RetrievalResult> results = knowledgeService.searchKnowledge(query, userId, topK, threshold, hybrid);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @GetMapping("/concepts")
    @Operation(summary = "List Open Knowledge Format (OKF v0.2) concepts in workspace")
    public ResponseEntity<ApiResponse<List<ConceptResponse>>> getConcepts(Authentication authentication) {
        String userId = authentication.getName();
        List<ConceptResponse> concepts = knowledgeService.listConcepts(userId);
        return ResponseEntity.ok(ApiResponse.ok(concepts));
    }

    @GetMapping("/concepts/{id}")
    @Operation(summary = "Get specific OKF v0.2 concept markdown and metadata")
    public ResponseEntity<ApiResponse<ConceptResponse>> getConcept(
            @PathVariable("id") String id,
            Authentication authentication) {

        String userId = authentication.getName();
        ConceptResponse concept = knowledgeService.getConcept(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(concept));
    }

    @GetMapping(value = "/export", produces = "application/zip")
    @Operation(summary = "Export entire knowledge base as an OKF v0.2 ZIP bundle with root index")
    public ResponseEntity<byte[]> exportBundle(Authentication authentication) throws IOException {
        String userId = authentication.getName();
        byte[] zipBytes = knowledgeService.exportBundleZip(userId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"knowledgehub-okf-bundle.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zipBytes);
    }
}
