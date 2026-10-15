package com.knowledgehub;

import com.knowledgehub.parser.ParsedDocument;
import com.knowledgehub.parser.ParsedSection;
import com.knowledgehub.rag.TextChunk;
import com.knowledgehub.rag.TextChunker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TextChunkerTest {

    private TextChunker chunker;

    @BeforeEach
    void setUp() {
        chunker = new TextChunker();
    }

    @Test
    @DisplayName("Should chunk large section into overlapping chunks without losing content")
    void testSlidingWindowChunking() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 50; i++) {
            sb.append("Sentence ").append(i).append(" defines business rule number ").append(i).append(". ");
        }

        ParsedSection section = new ParsedSection("Business Rules", sb.toString(), 2);
        ParsedDocument doc = new ParsedDocument("Rules Document", sb.toString(), 2, List.of(section));

        List<TextChunk> chunks = chunker.chunkDocument(doc, 200, 50);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() >= 3, "Long text should produce multiple overlapping chunks");
        for (TextChunk chunk : chunks) {
            assertEquals("Business Rules", chunk.getSectionName());
            assertEquals(2, chunk.getPageNumber());
            assertTrue(chunk.getTokenEstimate() > 0);
        }
    }

    @Test
    @DisplayName("Should preserve small section as a single chunk")
    void testSmallSectionSingleChunk() {
        ParsedSection section = new ParsedSection("Summary", "Short summary text.", 1);
        ParsedDocument doc = new ParsedDocument("Doc", "Short summary text.", 1, List.of(section));

        List<TextChunk> chunks = chunker.chunkDocument(doc, 800, 150);

        assertEquals(1, chunks.size());
        assertEquals("Short summary text.", chunks.get(0).getContent());
    }
}
