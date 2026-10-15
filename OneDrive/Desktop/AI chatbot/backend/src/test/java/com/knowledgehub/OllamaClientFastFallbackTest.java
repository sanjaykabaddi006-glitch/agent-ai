package com.knowledgehub;

import com.knowledgehub.config.KnowledgeHubProperties;
import com.knowledgehub.rag.OllamaClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OllamaClientFastFallbackTest {

    @Test
    @DisplayName("Should detect offline Ollama fast and generate fallback batch embeddings in milliseconds")
    void testFastFallbackBatchEmbeddings() {
        KnowledgeHubProperties properties = new KnowledgeHubProperties();
        properties.getOllama().setBaseUrl("http://127.0.0.1:59999"); // Port guaranteed to be offline
        properties.getOllama().setTimeoutSeconds(15);
        properties.getOllama().setMaxRetries(1);
        properties.getOllama().setMockFallbackEnabled(true);

        OllamaClient client = new OllamaClient(properties);

        long start = System.currentTimeMillis();
        int dimensions = client.getEmbeddingDimensions();
        assertEquals(768, dimensions);

        List<String> chunks = List.of(
                "First paragraph about enterprise financial compliance.",
                "Second paragraph detailing data privacy and GDPR regulations.",
                "Third paragraph covering vector database indexing procedures."
        );

        List<List<Double>> embeddings = client.generateBatchEmbeddings(chunks);
        long elapsed = System.currentTimeMillis() - start;

        assertEquals(3, embeddings.size());
        for (List<Double> vec : embeddings) {
            assertEquals(768, vec.size());
            // Check vector is not null and has non-zero elements
            double sumSq = vec.stream().mapToDouble(v -> v * v).sum();
            assertTrue(Math.abs(sumSq - 1.0) < 0.01, "Fallback embedding should be L2 normalized");
        }

        // Must complete very fast (typically < 1.6s for initial offline probe + ms for embeddings)
        assertTrue(elapsed < 4000, "Should complete fast under circuit breaker instead of hanging, took " + elapsed + "ms");
    }
}
