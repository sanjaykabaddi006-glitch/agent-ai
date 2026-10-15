package com.knowledgehub.parser;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class TxtDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String fileExtension) {
        return "txt".equalsIgnoreCase(fileExtension);
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String originalFilename) throws IOException {
        StringBuilder fullText = new StringBuilder();
        List<ParsedSection> sections = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            StringBuilder currentBlock = new StringBuilder();
            int sectionIndex = 1;

            while ((line = reader.readLine()) != null) {
                fullText.append(line).append("\n");

                if (line.isBlank()) {
                    if (currentBlock.length() > 0) {
                        sections.add(new ParsedSection("Section " + sectionIndex++, currentBlock.toString().trim(), null));
                        currentBlock = new StringBuilder();
                    }
                } else {
                    currentBlock.append(line).append("\n");
                }
            }

            if (currentBlock.length() > 0) {
                sections.add(new ParsedSection("Section " + sectionIndex, currentBlock.toString().trim(), null));
            }
        }

        if (sections.isEmpty() && fullText.length() > 0) {
            sections.add(new ParsedSection("Full Content", fullText.toString().trim(), 1));
        }

        return new ParsedDocument(originalFilename, fullText.toString().trim(), 1, sections);
    }
}
