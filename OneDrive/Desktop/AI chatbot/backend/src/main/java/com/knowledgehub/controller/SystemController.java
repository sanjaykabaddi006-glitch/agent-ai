package com.knowledgehub.controller;

import com.knowledgehub.common.ApiResponse;
import com.knowledgehub.dto.system.SystemStatusResponse;
import com.knowledgehub.service.SystemStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "System", description = "System health, Ollama AI connectivity, and model discovery")
public class SystemController {

    private final SystemStatusService systemStatusService;

    public SystemController(SystemStatusService systemStatusService) {
        this.systemStatusService = systemStatusService;
    }

    @GetMapping("/health")
    @Operation(summary = "Basic health check")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "UP", "service", "KnowledgeHub AI")));
    }

    @GetMapping("/ai/status")
    @Operation(summary = "Get local Ollama connectivity, models, and ingestion statistics")
    public ResponseEntity<ApiResponse<SystemStatusResponse>> aiStatus(Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : null;
        SystemStatusResponse status = systemStatusService.getSystemStatus(userId);
        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    @GetMapping("/ai/models")
    @Operation(summary = "List locally available models pulled in Ollama")
    public ResponseEntity<ApiResponse<List<String>>> aiModels() {
        List<String> models = systemStatusService.getAvailableModels();
        return ResponseEntity.ok(ApiResponse.ok(models));
    }
}
