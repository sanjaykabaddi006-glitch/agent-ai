package com.knowledgehub.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledgehub.config.KnowledgeHubProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class OllamaClient implements OllamaChatService, OllamaEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long HEALTH_CHECK_TTL_MS = 60_000L; // Cache reachability for 60s

    private final KnowledgeHubProperties properties;
    private final HttpClient httpClient;
    private Integer detectedEmbeddingDimensions = null;
    private volatile Boolean cachedConnected = null;
    private volatile long lastHealthCheckTimeMs = 0;

    public OllamaClient(KnowledgeHubProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2)) // Fast fail on unreachable local server
                .build();
    }

    public boolean isConnected() {
        long now = System.currentTimeMillis();
        if (cachedConnected != null && (now - lastHealthCheckTimeMs) < HEALTH_CHECK_TTL_MS) {
            return cachedConnected;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getOllama().getBaseUrl() + "/api/tags"))
                    .timeout(Duration.ofMillis(1500))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            boolean ok = response.statusCode() == 200;
            cachedConnected = ok;
            lastHealthCheckTimeMs = now;
            return ok;
        } catch (Exception e) {
            cachedConnected = false;
            lastHealthCheckTimeMs = now;
            return false;
        }
    }

    public List<String> getAvailableModels() {
        List<String> modelNames = new ArrayList<>();
        if (!isConnected()) {
            return modelNames;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getOllama().getBaseUrl() + "/api/tags"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                JsonNode root = MAPPER.readTree(response.body());
                JsonNode models = root.get("models");
                if (models != null && models.isArray()) {
                    for (JsonNode m : models) {
                        if (m.has("name")) {
                            modelNames.add(m.get("name").asText());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Ollama getAvailableModels unreachable: {}", e.getMessage());
        }
        return modelNames;
    }

    @Override
    public int getEmbeddingDimensions() {
        if (detectedEmbeddingDimensions != null) {
            return detectedEmbeddingDimensions;
        }

        if (isConnected()) {
            try {
                List<Double> probe = generateEmbedding("dimension probe");
                if (probe != null && !probe.isEmpty()) {
                    detectedEmbeddingDimensions = probe.size();
                    log.info("Detected embedding dimensions for model '{}': {}", properties.getOllama().getEmbeddingModel(), detectedEmbeddingDimensions);
                    return detectedEmbeddingDimensions;
                }
            } catch (Exception e) {
                log.warn("Could not determine embedding dimensions from Ollama: {}", e.getMessage());
            }
        }

        // Standard default dimension for nomic-embed-text
        detectedEmbeddingDimensions = 768;
        return detectedEmbeddingDimensions;
    }

    @Override
    public List<Double> generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        if (!isConnected()) {
            if (properties.getOllama().isMockFallbackEnabled()) {
                return generateDeterministicFallbackEmbedding(text, 768);
            }
            throw new RuntimeException("Ollama service unavailable and fallback is disabled.");
        }

        String baseUrl = properties.getOllama().getBaseUrl();
        String model = properties.getOllama().getEmbeddingModel();

        for (int attempt = 0; attempt <= properties.getOllama().getMaxRetries(); attempt++) {
            try {
                Map<String, Object> bodyMap = Map.of(
                        "model", model,
                        "prompt", text,
                        "keep_alive", "30m"
                );
                String jsonBody = MAPPER.writeValueAsString(bodyMap);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/embeddings"))
                        .timeout(Duration.ofSeconds(Math.min(properties.getOllama().getTimeoutSeconds(), 15)))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() == 200) {
                    JsonNode node = MAPPER.readTree(response.body());
                    JsonNode embeddingNode = node.get("embedding");
                    if (embeddingNode != null && embeddingNode.isArray()) {
                        List<Double> vector = new ArrayList<>();
                        for (JsonNode val : embeddingNode) {
                            vector.add(val.asDouble());
                        }
                        return vector;
                    }
                }
            } catch (Exception e) {
                log.warn("Ollama embedding call attempt {} failed: {}", attempt + 1, e.getMessage());
                cachedConnected = false;
                lastHealthCheckTimeMs = System.currentTimeMillis();
            }
        }

        if (properties.getOllama().isMockFallbackEnabled()) {
            log.info("Using deterministic fallback embedding generator for text (length: {})", text.length());
            return generateDeterministicFallbackEmbedding(text, 768);
        }

        throw new RuntimeException("Ollama service unavailable and fallback is disabled.");
    }

    @Override
    public List<List<Double>> generateBatchEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        if (!isConnected()) {
            if (properties.getOllama().isMockFallbackEnabled()) {
                List<List<Double>> fallbackResults = new ArrayList<>(texts.size());
                for (String t : texts) {
                    fallbackResults.add(generateDeterministicFallbackEmbedding(t, 768));
                }
                return fallbackResults;
            }
            throw new RuntimeException("Ollama service unavailable and fallback is disabled.");
        }

        // Try Ollama batch /api/embed endpoint first
        try {
            Map<String, Object> bodyMap = Map.of(
                    "model", properties.getOllama().getEmbeddingModel(),
                    "input", texts,
                    "keep_alive", "30m"
            );
            String jsonBody = MAPPER.writeValueAsString(bodyMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getOllama().getBaseUrl() + "/api/embed"))
                    .timeout(Duration.ofSeconds(Math.min(properties.getOllama().getTimeoutSeconds(), 30)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                JsonNode root = MAPPER.readTree(response.body());
                JsonNode embeddingsNode = root.get("embeddings");
                if (embeddingsNode != null && embeddingsNode.isArray()) {
                    List<List<Double>> allVectors = new ArrayList<>();
                    for (JsonNode arr : embeddingsNode) {
                        List<Double> vec = new ArrayList<>();
                        for (JsonNode d : arr) {
                            vec.add(d.asDouble());
                        }
                        allVectors.add(vec);
                    }
                    if (allVectors.size() == texts.size()) {
                        return allVectors;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Ollama /api/embed batch endpoint call failed or not supported, falling back to parallel requests: {}", e.getMessage());
        }

        // Fallback: Parallel requests using worker pool
        int threadPoolSize = Math.min(texts.size(), 6);
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        try {
            List<CompletableFuture<List<Double>>> futures = texts.stream()
                    .map(t -> CompletableFuture.supplyAsync(() -> generateEmbedding(t), executor))
                    .toList();
            return futures.stream().map(CompletableFuture::join).toList();
        } finally {
            executor.shutdown();
        }
    }

    @Override
    public String generateAnswer(String systemPrompt, String userPrompt, List<ChatMessageDto> history) {
        return generateAnswer(systemPrompt, userPrompt, history, 120);
    }

    @Override
    public String generateAnswer(String systemPrompt, String userPrompt, List<ChatMessageDto> history, Integer numPredict) {
        int maxTokens = (numPredict != null && numPredict > 0) ? numPredict : 120;
        String baseUrl = properties.getOllama().getBaseUrl();
        String model = properties.getOllama().getChatModel();

        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }

        if (history != null && !history.isEmpty()) {
            int startIdx = Math.max(0, history.size() - 4);
            for (int i = startIdx; i < history.size(); i++) {
                ChatMessageDto h = history.get(i);
                messages.add(Map.of("role", h.role().toLowerCase(), "content", h.content()));
            }
        }

        messages.add(Map.of("role", "user", "content", userPrompt));

        if (!isConnected()) {
            if (properties.getOllama().isMockFallbackEnabled()) {
                log.info("Ollama is offline or unreachable. Producing graceful fallback answer based on prompt context.");
                return generateMockGroundedAnswer(systemPrompt, userPrompt);
            }
            throw new RuntimeException("Ollama chat service is unreachable and fallback mode is disabled.");
        }

        for (int attempt = 0; attempt <= Math.min(properties.getOllama().getMaxRetries(), 0); attempt++) {
            try {
                int ctxSize = maxTokens > 500 ? 4096 : 2048;
                int numThreads = Math.max(Runtime.getRuntime().availableProcessors() - 2, 4);
                Map<String, Object> optionsMap = Map.of(
                        "temperature", 0.0,
                        "num_predict", maxTokens,
                        "top_p", 0.8,
                        "num_ctx", ctxSize,
                        "num_thread", numThreads
                );
                Map<String, Object> bodyMap = Map.of(
                        "model", model,
                        "messages", messages,
                        "stream", false,
                        "keep_alive", "30m",
                        "options", optionsMap
                );
                String jsonBody = MAPPER.writeValueAsString(bodyMap);

                int requestTimeout = Math.max(properties.getOllama().getTimeoutSeconds(), 180);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/chat"))
                        .timeout(Duration.ofSeconds(requestTimeout))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() == 200) {
                    JsonNode node = MAPPER.readTree(response.body());
                    JsonNode messageNode = node.get("message");
                    if (messageNode != null && messageNode.has("content")) {
                        return messageNode.get("content").asText();
                    }
                }
            } catch (Exception e) {
                log.warn("Ollama chat generation attempt {} failed: {}", attempt + 1, e.getMessage());
            }
        }

        if (properties.getOllama().isMockFallbackEnabled()) {
            log.info("Ollama is offline or busy. Producing direct targeted answer from retrieved context.");
            return generateMockGroundedAnswer(systemPrompt, userPrompt);
        }

        throw new RuntimeException("Ollama chat service is unreachable and fallback mode is disabled.");
    }

    private List<Double> generateDeterministicFallbackEmbedding(String text, int dimensions) {
        long seed = (long) text.hashCode();
        Random random = new Random(seed);
        List<Double> vector = new ArrayList<>(dimensions);
        double normSq = 0.0;

        for (int i = 0; i < dimensions; i++) {
            double val = random.nextGaussian();
            vector.add(val);
            normSq += val * val;
        }

        // L2 normalize vector
        double norm = Math.sqrt(normSq);
        if (norm > 0) {
            for (int i = 0; i < dimensions; i++) {
                vector.set(i, vector.get(i) / norm);
            }
        }
        return vector;
    }

    private String generateMockGroundedAnswer(String systemPrompt, String userPrompt) {
        if (systemPrompt != null && systemPrompt.contains("CONTEXT:")) {
            String contextPart = systemPrompt.substring(systemPrompt.indexOf("CONTEXT:"));

            // If user asked for full document details, provide full structured context lines
            if (VectorSearchService.isFullDocumentRequest(userPrompt)) {
                String[] lines = contextPart.split("\n");
                List<String> contentLines = new ArrayList<>();
                for (String line : lines) {
                    String clean = line.trim();
                    if (!clean.isBlank() && !clean.startsWith("CONTEXT:") && !clean.startsWith("[")) {
                        contentLines.add(clean);
                    }
                }
                if (!contentLines.isEmpty()) {
                    return String.join("\n", contentLines);
                }
            }

            Set<String> keywords = Arrays.stream(userPrompt.toLowerCase().split("\\W+"))
                    .filter(w -> w.length() > 2)
                    .collect(java.util.stream.Collectors.toSet());

            String[] lines = contextPart.split("\n");
            List<String> matchingLines = new ArrayList<>();
            for (String line : lines) {
                String clean = line.trim();
                if (clean.isBlank() || clean.startsWith("CONTEXT:") || clean.startsWith("[") || clean.startsWith("===") || clean.startsWith("---")) {
                    continue;
                }
                String lower = clean.toLowerCase();
                for (String kw : keywords) {
                    if (lower.contains(kw)) {
                        matchingLines.add(clean);
                        break;
                    }
                }
                if (matchingLines.size() >= 10) break;
            }

            if (!matchingLines.isEmpty()) {
                return String.join("\n", matchingLines);
            }
        }

        return "The uploaded document does not contain this information. I can only answer questions based on the content of your uploaded document.";
    }
}
