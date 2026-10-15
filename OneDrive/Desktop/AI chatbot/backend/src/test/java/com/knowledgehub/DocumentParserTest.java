package com.knowledgehub;

import com.knowledgehub.parser.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentParserTest {

    private DocumentParserFactory parserFactory;
    private MarkdownDocumentParser markdownParser;
    private TxtDocumentParser txtParser;
    private CsvDocumentParser csvParser;
    private PptDocumentParser pptParser;

    @BeforeEach
    void setUp() {
        markdownParser = new MarkdownDocumentParser();
        txtParser = new TxtDocumentParser();
        csvParser = new CsvDocumentParser();
        pptParser = new PptDocumentParser();
        parserFactory = new DocumentParserFactory(List.of(markdownParser, txtParser, csvParser, pptParser));
    }

    @Test
    @DisplayName("Should parse Markdown document into sections and extract title")
    void testMarkdownParser() throws IOException {
        String mdContent = """
                # Enterprise Architecture Guidelines
                
                This document defines the high-level system architecture.
                
                ## Security Requirements
                
                All endpoints must enforce JWT bearer token authentication.
                
                ## Persistence Strategy
                
                PostgreSQL with pgvector is used for persistent vector embeddings.
                """;

        ByteArrayInputStream is = new ByteArrayInputStream(mdContent.getBytes(StandardCharsets.UTF_8));
        ParsedDocument doc = markdownParser.parse(is, "architecture.md");

        assertNotNull(doc);
        assertEquals("Enterprise Architecture Guidelines", doc.getTitle());
        assertTrue(doc.getSections().size() >= 2);
        assertTrue(doc.getFullText().contains("PostgreSQL with pgvector"));
    }

    @Test
    @DisplayName("Should parse TXT document with paragraph sectioning")
    void testTxtParser() throws IOException {
        String txtContent = "Paragraph one with critical business context.\n\nParagraph two with procedural details.";
        ByteArrayInputStream is = new ByteArrayInputStream(txtContent.getBytes(StandardCharsets.UTF_8));
        ParsedDocument doc = txtParser.parse(is, "policy.txt");

        assertNotNull(doc);
        assertEquals("policy.txt", doc.getTitle());
        assertEquals(2, doc.getSections().size());
    }

    @Test
    @DisplayName("Should parse CSV document into structured rows")
    void testCsvParser() throws IOException {
        String csvContent = "ID,Name,Department\n1,Alice,Engineering\n2,Bob,Product";
        ByteArrayInputStream is = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        ParsedDocument doc = csvParser.parse(is, "employees.csv");

        assertNotNull(doc);
        assertTrue(doc.getFullText().contains("Name = Alice"));
        assertTrue(doc.getFullText().contains("Department = Product"));
    }

    @Test
    @DisplayName("Factory should resolve appropriate parser by filename extension")
    void testParserFactory() {
        DocumentParser parserMd = parserFactory.getParser("test.md");
        assertTrue(parserMd instanceof MarkdownDocumentParser);

        DocumentParser parserTxt = parserFactory.getParser("data.txt");
        assertTrue(parserTxt instanceof TxtDocumentParser);

        DocumentParser parserCsv = parserFactory.getParser("records.csv");
        assertTrue(parserCsv instanceof CsvDocumentParser);

        DocumentParser parserPptx = parserFactory.getParser("presentation.pptx");
        assertTrue(parserPptx instanceof PptDocumentParser);

        DocumentParser parserPpt = parserFactory.getParser("legacy.ppt");
        assertTrue(parserPpt instanceof PptDocumentParser);
    }

    @Test
    @DisplayName("Should parse PPTX document into slides and extract content")
    void testPptxParser() throws IOException {
        org.apache.poi.xslf.usermodel.XMLSlideShow pptx = new org.apache.poi.xslf.usermodel.XMLSlideShow();
        org.apache.poi.xslf.usermodel.XSLFSlide slide = pptx.createSlide();
        org.apache.poi.xslf.usermodel.XSLFTextBox shape = slide.createTextBox();
        org.apache.poi.xslf.usermodel.XSLFTextParagraph p = shape.addNewTextParagraph();
        org.apache.poi.xslf.usermodel.XSLFTextRun r = p.addNewTextRun();
        r.setText("Artificial Intelligence and Vector Search Overview");

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        pptx.write(out);
        pptx.close();

        ByteArrayInputStream is = new ByteArrayInputStream(out.toByteArray());
        ParsedDocument doc = pptParser.parse(is, "ai_overview.pptx");

        assertNotNull(doc);
        assertEquals("ai_overview.pptx", doc.getTitle());
        assertTrue(doc.getSections().size() >= 1);
        assertTrue(doc.getFullText().contains("Artificial Intelligence and Vector Search Overview"));
    }
}
