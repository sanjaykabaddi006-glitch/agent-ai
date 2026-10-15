package com.knowledgehub.parser;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class DocxDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String fileExtension) {
        return "docx".equalsIgnoreCase(fileExtension);
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String originalFilename) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(inputStream)) {
            StringBuilder fullText = new StringBuilder();
            List<ParsedSection> sections = new ArrayList<>();

            String currentSectionTitle = "Document Start";
            StringBuilder currentSectionContent = new StringBuilder();

            for (XWPFParagraph p : doc.getParagraphs()) {
                String text = p.getText().trim();
                if (text.isEmpty()) {
                    continue;
                }

                // If paragraph is a heading or starts like a heading
                boolean isHeading = p.getStyle() != null && p.getStyle().toLowerCase().startsWith("heading")
                        || (text.length() < 80 && (text.startsWith("Chapter") || text.startsWith("Section") || text.matches("^[0-9]+(\\.[0-9]+)*\\s+.*")));

                if (isHeading) {
                    if (currentSectionContent.length() > 0) {
                        sections.add(new ParsedSection(currentSectionTitle, currentSectionContent.toString().trim(), null));
                        currentSectionContent = new StringBuilder();
                    }
                    currentSectionTitle = text;
                }

                currentSectionContent.append(text).append("\n");
                fullText.append(text).append("\n\n");
            }

            // Also extract tables if any
            for (XWPFTable table : doc.getTables()) {
                StringBuilder tableText = new StringBuilder("\n[Table]\n");
                for (XWPFTableRow row : table.getRows()) {
                    List<String> cells = new ArrayList<>();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        cells.add(cell.getText().trim());
                    }
                    tableText.append(String.join(" | ", cells)).append("\n");
                }
                currentSectionContent.append(tableText).append("\n");
                fullText.append(tableText).append("\n");
            }

            if (currentSectionContent.length() > 0) {
                sections.add(new ParsedSection(currentSectionTitle, currentSectionContent.toString().trim(), null));
            }

            if (sections.isEmpty() && fullText.length() > 0) {
                sections.add(new ParsedSection("General Content", fullText.toString().trim(), 1));
            }

            return new ParsedDocument(originalFilename, fullText.toString().trim(), 1, sections);
        }
    }
}
