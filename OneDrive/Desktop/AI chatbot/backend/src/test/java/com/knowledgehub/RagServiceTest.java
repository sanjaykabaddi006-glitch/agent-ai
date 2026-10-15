package com.knowledgehub;

import com.knowledgehub.rag.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class RagServiceTest {

    private OllamaEmbeddingService embeddingService;
    private OllamaChatService chatService;
    private VectorSearchService vectorSearchService;
    private RagService ragService;

    @BeforeEach
    void setUp() {
        embeddingService = Mockito.mock(OllamaEmbeddingService.class);
        chatService = Mockito.mock(OllamaChatService.class);
        vectorSearchService = Mockito.mock(VectorSearchService.class);
        ragService = new RagService(embeddingService, chatService, vectorSearchService);
    }

    @Test
    @DisplayName("Should return insufficient evidence when no chunks match query threshold")
    void testInsufficientEvidenceRefusal() {
        when(embeddingService.generateEmbedding(anyString())).thenReturn(List.of(0.1, 0.2, 0.3));
        when(vectorSearchService.search(any(), anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(vectorSearchService.search(any(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(List.of());

        RagResponse response = ragService.query("What is the quarterly bonus formula?", "user-1", List.of(), 5, 0.5, true);

        assertNotNull(response);
        assertFalse(response.isGrounded());
        assertTrue(response.getCitations().isEmpty());
        assertTrue(response.getAnswer().contains("do not provide enough information"));
    }

    @Test
    @DisplayName("Should ground answer and attach verifiable citations when evidence exists")
    void testGroundedResponseWithCitations() {
        when(embeddingService.generateEmbedding(anyString())).thenReturn(List.of(0.1, 0.2, 0.3));

        RetrievalResult mockResult = new RetrievalResult(
                "chunk-1",
                "doc-1",
                "Employee Handbook",
                "The standard working hours are 9:00 AM to 5:00 PM Monday through Friday.",
                4,
                "Working Hours",
                0,
                0.89
        );

        when(vectorSearchService.search(any(), anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(List.of(mockResult));
        when(vectorSearchService.search(any(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(List.of(mockResult));

        when(chatService.generateAnswer(anyString(), anyString(), anyList()))
                .thenReturn("Standard working hours are 9:00 AM to 5:00 PM Monday to Friday based on the Employee Handbook.");
        when(chatService.generateAnswer(anyString(), anyString(), anyList(), any()))
                .thenReturn("Standard working hours are 9:00 AM to 5:00 PM Monday to Friday based on the Employee Handbook.");

        RagResponse response = ragService.query("What are the office hours?", "user-1", List.of(), 5, 0.5, true);

        assertNotNull(response);
        assertTrue(response.isGrounded());
        assertEquals(1, response.getCitations().size());
        Citation citation = response.getCitations().get(0);
        assertEquals("Employee Handbook", citation.getDocumentTitle());
        assertEquals(4, citation.getPageNumber());
        assertEquals("Working Hours", citation.getSectionName());
        assertTrue(response.getAnswer().contains("9:00 AM to 5:00 PM"));
    }

    @Test
    @DisplayName("Should detect full document NLP request and assemble comprehensive multi-chunk overview")
    void testFullDocumentRequestNLP() {
        RetrievalResult chunk1 = new RetrievalResult("c-1", "doc-1", "Resume", "Sanjay G - BE CSE", 1, "PROFILE", 0, 1.0);
        RetrievalResult chunk2 = new RetrievalResult("c-2", "doc-1", "Resume", "Skills: Java, Python", 1, "SKILLS", 1, 1.0);

        when(vectorSearchService.search(any(), anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(List.of(chunk1, chunk2));
        when(vectorSearchService.search(any(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(List.of(chunk1, chunk2));

        when(chatService.generateAnswer(anyString(), anyString(), anyList(), any()))
                .thenReturn("### Candidate Summary\n- Degree: BE CSE\n- Technical Skills: Java, Python");

        RagResponse response = ragService.query("give full document details of the resume", "user-1", List.of(), 5, 0.5, true);

        assertNotNull(response);
        assertTrue(response.isGrounded());
        assertEquals(2, response.getCitations().size());
        assertTrue(response.getAnswer().contains("Candidate Summary"));
    }

    @Test
    @DisplayName("Should detect 'explain full pdf' and allocate 4096 token budget without cutting off")
    void testExplainFullPdf() {
        assertTrue(VectorSearchService.isFullDocumentRequest("explain full pdf"));
        assertTrue(VectorSearchService.isFullDocumentRequest("give that file full content"));
        assertTrue(VectorSearchService.isFullDocumentRequest("explain this pdf"));
        assertTrue(VectorSearchService.isFullDocumentRequest("what can upload give full co tent means give"));

        RetrievalResult chunk1 = new RetrievalResult("c-1", "doc-1", "Hackathon Problem", "Smart Lorry Load & Route Optimization System", 1, "REQ", 0, 1.0);
        when(vectorSearchService.search(any(), anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(List.of(chunk1));
        when(vectorSearchService.search(any(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(List.of(chunk1));

        when(chatService.generateAnswer(anyString(), anyString(), anyList(), eq(4096)))
                .thenReturn("Complete problem statement and all requirements without cutoff.");

        RagResponse response = ragService.query("explain full pdf", "user-1", List.of(), 5, 0.5, true);
        assertNotNull(response);
        assertTrue(response.isGrounded());
        assertEquals("Complete problem statement and all requirements without cutoff.", response.getAnswer());
    }
}
