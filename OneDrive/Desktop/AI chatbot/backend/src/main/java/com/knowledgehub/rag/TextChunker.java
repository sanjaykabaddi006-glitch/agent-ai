package com.knowledgehub.rag;

import com.knowledgehub.parser.ParsedDocument;
import com.knowledgehub.parser.ParsedSection;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    public List<TextChunk> chunkDocument(ParsedDocument document, int chunkSize, int chunkOverlap) {
        List<TextChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;

        if (chunkSize <= 0) chunkSize = 800;
        if (chunkOverlap < 0 || chunkOverlap >= chunkSize) chunkOverlap = 150;

        List<ParsedSection> sections = document.getSections();
        if (sections != null && !sections.isEmpty()) {
            for (ParsedSection section : sections) {
                String text = section.getContent();
                if (text == null || text.isBlank()) continue;

                List<String> textPieces = splitTextWithOverlap(text, chunkSize, chunkOverlap);
                for (String piece : textPieces) {
                    if (!piece.isBlank()) {
                        chunks.add(new TextChunk(
                                chunkIndex++,
                                piece.trim(),
                                section.getPageNumber(),
                                section.getTitle()
                        ));
                    }
                }
            }
        }

        // If no sections produced chunks, chunk full text
        if (chunks.isEmpty() && document.getFullText() != null && !document.getFullText().isBlank()) {
            List<String> pieces = splitTextWithOverlap(document.getFullText(), chunkSize, chunkOverlap);
            for (String piece : pieces) {
                if (!piece.isBlank()) {
                    chunks.add(new TextChunk(
                            chunkIndex++,
                            piece.trim(),
                            1,
                            "General Content"
                    ));
                }
            }
        }

        return chunks;
    }

    private List<String> splitTextWithOverlap(String text, int chunkSize, int chunkOverlap) {
        List<String> results = new ArrayList<>();
        if (text.length() <= chunkSize) {
            results.add(text);
            return results;
        }

        int step = chunkSize - chunkOverlap;
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            // If we are not at the end of the text, try to break at a sentence or word boundary
            if (end < text.length()) {
                int boundary = findSentenceOrWordBoundary(text, end, start);
                if (boundary > start + (chunkSize / 2)) {
                    end = boundary;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                results.add(chunk);
            }

            if (end >= text.length()) {
                break;
            }

            start += step;
        }

        return results;
    }

    private int findSentenceOrWordBoundary(String text, int end, int start) {
        // Look backwards up to 100 characters for sentence termination (.!?)
        for (int i = end; i > Math.max(start, end - 80); i--) {
            char c = text.charAt(i - 1);
            if ((c == '.' || c == '?' || c == '!' || c == '\n') && (i == text.length() || Character.isWhitespace(text.charAt(i)))) {
                return i;
            }
        }
        // Otherwise look for whitespace
        for (int i = end; i > Math.max(start, end - 50); i--) {
            if (Character.isWhitespace(text.charAt(i - 1))) {
                return i;
            }
        }
        return end;
    }
}
