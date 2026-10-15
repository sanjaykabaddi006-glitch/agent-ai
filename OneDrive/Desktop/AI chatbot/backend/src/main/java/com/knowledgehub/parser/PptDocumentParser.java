package com.knowledgehub.parser;

import org.apache.poi.hslf.usermodel.*;
import org.apache.poi.xslf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class PptDocumentParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(PptDocumentParser.class);

    @Override
    public boolean supports(String fileExtension) {
        return "pptx".equalsIgnoreCase(fileExtension) || "ppt".equalsIgnoreCase(fileExtension);
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String originalFilename) throws IOException {
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }

        byte[] bytes = inputStream.readAllBytes();
        if ("ppt".equalsIgnoreCase(ext)) {
            return parseHslf(bytes, originalFilename);
        } else {
            return parseXslf(bytes, originalFilename);
        }
    }

    private ParsedDocument parseXslf(byte[] bytes, String originalFilename) throws IOException {
        try (InputStream is = new ByteArrayInputStream(bytes);
             XMLSlideShow pptx = new XMLSlideShow(is)) {

            StringBuilder fullText = new StringBuilder();
            List<ParsedSection> sections = new ArrayList<>();
            List<XSLFSlide> slides = pptx.getSlides();
            int pageCount = Math.max(1, slides.size());

            for (int i = 0; i < slides.size(); i++) {
                XSLFSlide slide = slides.get(i);
                int slideNum = i + 1;
                String slideTitle = slide.getTitle();
                if (slideTitle == null || slideTitle.isBlank()) {
                    slideTitle = "Slide " + slideNum;
                }

                StringBuilder slideContent = new StringBuilder();

                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String txt = textShape.getText();
                        if (txt != null && !txt.isBlank()) {
                            slideContent.append(txt.trim()).append("\n");
                        }
                    } else if (shape instanceof XSLFTable table) {
                        StringBuilder tableText = new StringBuilder("\n[Table]\n");
                        for (XSLFTableRow row : table.getRows()) {
                            List<String> cells = new ArrayList<>();
                            for (XSLFTableCell cell : row.getCells()) {
                                cells.add(cell.getText().trim());
                            }
                            tableText.append(String.join(" | ", cells)).append("\n");
                        }
                        slideContent.append(tableText).append("\n");
                    }
                }

                String content = slideContent.toString().trim();
                if (!content.isEmpty()) {
                    sections.add(new ParsedSection(slideTitle, content, slideNum));
                    fullText.append("=== ").append(slideTitle).append(" ===\n").append(content).append("\n\n");
                }
            }

            if (sections.isEmpty()) {
                sections.add(new ParsedSection("Presentation", "No text found in presentation slides.", 1));
                fullText.append("No text found in presentation slides.");
            }

            return new ParsedDocument(originalFilename, fullText.toString().trim(), pageCount, sections);
        } catch (Exception e) {
            log.warn("Failed parsing as PPTX, attempting legacy HSLF PPT parser: {}", e.getMessage());
            return parseHslf(bytes, originalFilename);
        }
    }

    private ParsedDocument parseHslf(byte[] bytes, String originalFilename) throws IOException {
        try (InputStream is = new ByteArrayInputStream(bytes);
             HSLFSlideShow ppt = new HSLFSlideShow(is)) {

            StringBuilder fullText = new StringBuilder();
            List<ParsedSection> sections = new ArrayList<>();
            List<HSLFSlide> slides = ppt.getSlides();
            int pageCount = Math.max(1, slides.size());

            for (int i = 0; i < slides.size(); i++) {
                HSLFSlide slide = slides.get(i);
                int slideNum = i + 1;
                String slideTitle = slide.getTitle();
                if (slideTitle == null || slideTitle.isBlank()) {
                    slideTitle = "Slide " + slideNum;
                }

                StringBuilder slideContent = new StringBuilder();

                for (HSLFShape shape : slide.getShapes()) {
                    if (shape instanceof HSLFTextShape textShape) {
                        String txt = textShape.getText();
                        if (txt != null && !txt.isBlank()) {
                            slideContent.append(txt.trim()).append("\n");
                        }
                    } else if (shape instanceof HSLFTable table) {
                        StringBuilder tableText = new StringBuilder("\n[Table]\n");
                        for (int r = 0; r < table.getNumberOfRows(); r++) {
                            List<String> cells = new ArrayList<>();
                            for (int c = 0; c < table.getNumberOfColumns(); c++) {
                                HSLFTableCell cell = table.getCell(r, c);
                                cells.add(cell != null ? cell.getText().trim() : "");
                            }
                            tableText.append(String.join(" | ", cells)).append("\n");
                        }
                        slideContent.append(tableText).append("\n");
                    }
                }

                String content = slideContent.toString().trim();
                if (!content.isEmpty()) {
                    sections.add(new ParsedSection(slideTitle, content, slideNum));
                    fullText.append("=== ").append(slideTitle).append(" ===\n").append(content).append("\n\n");
                }
            }

            if (sections.isEmpty()) {
                sections.add(new ParsedSection("Presentation", "No text found in presentation slides.", 1));
                fullText.append("No text found in presentation slides.");
            }

            return new ParsedDocument(originalFilename, fullText.toString().trim(), pageCount, sections);
        }
    }
}
