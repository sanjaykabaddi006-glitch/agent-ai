package com.knowledgehub.rag;

import java.util.List;

public interface OllamaEmbeddingService {
    List<Double> generateEmbedding(String text);
    List<List<Double>> generateBatchEmbeddings(List<String> texts);
    int getEmbeddingDimensions();
}
