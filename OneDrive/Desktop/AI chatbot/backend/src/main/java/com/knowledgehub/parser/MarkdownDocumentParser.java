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
public class MarkdownDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String fileExtension) {
        return "md".equalsIgnoreCase(fileExtension) || "markdown".equalsIgnoreCase(fileExtension);
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String originalFilename) throws IOException {
        StringBuilder fullText = new StringBuilder();
        List<ParsedSection> sections = new ArrayList<>();

        String docTitle = originalFilename;
        String currentSectionTitle = "Overview";
        StringBuilder currentSectionContent = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            boolean firstHeadingFound = false;

            while ((line = reader.readLine()) != null) {
                fullText.append(line).append("\n");

                if (line.startsWith("#")) {
                    String headingText = line.replaceFirst("^#+\\s*", "").trim();

                    if (!firstHeadingFound && line.startsWith("# ")) {
                        docTitle = headingText;
                        firstHeadingFound = true;
                    }

                    if (currentSectionContent.length() > 0) {
                        sections.add(new ParsedSection(currentSectionTitle, currentSectionContent.toString().trim(), null));
                        currentSectionContent = new StringBuilder();
                    }
                    currentSectionTitle = headingText;
                } else {
                    currentSectionContent.append(line).append("\n");
                }
            }

            if (currentSectionContent.length() > 0) {
                sections.add(new ParsedSection(currentSectionTitle, currentSectionContent.toString().trim(), null));
            }
        }

        if (sections.isEmpty() && fullText.length() > 0) {
            sections.add(new ParsedSection("Overview", fullText.toString().trim(), 1));
        }

        return new ParsedDocument(docTitle, fullText.toString().trim(), 1, sections);
    }
}
