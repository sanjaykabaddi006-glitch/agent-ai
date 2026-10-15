package com.knowledgehub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "knowledgehub")
public class KnowledgeHubProperties {

    private Storage storage = new Storage();
    private Jwt jwt = new Jwt();
    private Ollama ollama = new Ollama();
    private Rag rag = new Rag();

    public Storage getStorage() { return storage; }
    public void setStorage(Storage storage) { this.storage = storage; }

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }

    public Ollama getOllama() { return ollama; }
    public void setOllama(Ollama ollama) { this.ollama = ollama; }

    public Rag getRag() { return rag; }
    public void setRag(Rag rag) { this.rag = rag; }

    public static class Storage {
        private String uploadDir = "./data/uploads";
        private String okfDir = "./data/okf";
        private long maxFileSizeBytes = 26214400L; // 25MB
        private List<String> allowedExtensions = List.of("pdf", "docx", "ppt", "pptx", "txt", "md", "csv");

        public String getUploadDir() { return uploadDir; }
        public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }

        public String getOkfDir() { return okfDir; }
        public void setOkfDir(String okfDir) { this.okfDir = okfDir; }

        public long getMaxFileSizeBytes() { return maxFileSizeBytes; }
        public void setMaxFileSizeBytes(long maxFileSizeBytes) { this.maxFileSizeBytes = maxFileSizeBytes; }

        public List<String> getAllowedExtensions() { return allowedExtensions; }
        public void setAllowedExtensions(List<String> allowedExtensions) { this.allowedExtensions = allowedExtensions; }
    }

    public static class Jwt {
        private String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970337336763979244226452948404D6351";
        private long expirationMs = 86400000L; // 24h

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }

        public long getExpirationMs() { return expirationMs; }
        public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }
    }

    public static class Ollama {
        private String baseUrl = "http://localhost:11434";
        private String chatModel = "llama3.2";
        private String embeddingModel = "nomic-embed-text";
        private int timeoutSeconds = 45;
        private int maxRetries = 2;
        private boolean mockFallbackEnabled = true;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public String getChatModel() { return chatModel; }
        public void setChatModel(String chatModel) { this.chatModel = chatModel; }

        public String getEmbeddingModel() { return embeddingModel; }
        public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }

        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

        public boolean isMockFallbackEnabled() { return mockFallbackEnabled; }
        public void setMockFallbackEnabled(boolean mockFallbackEnabled) { this.mockFallbackEnabled = mockFallbackEnabled; }
    }

    public static class Rag {
        private int chunkSize = 800;
        private int chunkOverlap = 150;
        private int batchSize = 10;
        private int topK = 5;
        private double similarityThreshold = 0.20;
        private boolean hybridSearch = true;

        public int getChunkSize() { return chunkSize; }
        public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }

        public int getChunkOverlap() { return chunkOverlap; }
        public void setChunkOverlap(int chunkOverlap) { this.chunkOverlap = chunkOverlap; }

        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

        public int getTopK() { return topK; }
        public void setTopK(int topK) { this.topK = topK; }

        public double getSimilarityThreshold() { return similarityThreshold; }
        public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }

        public boolean isHybridSearch() { return hybridSearch; }
        public void setHybridSearch(boolean hybridSearch) { this.hybridSearch = hybridSearch; }
    }
}
