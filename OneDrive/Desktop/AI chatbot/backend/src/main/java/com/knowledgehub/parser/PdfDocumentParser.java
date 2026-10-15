package com.knowledgehub.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String fileExtension) {
        return "pdf".equalsIgnoreCase(fileExtension);
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String originalFilename) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        if (bytes.length == 0) {
            throw new IOException("Cannot parse empty PDF document");
        }

        try (PDDocument document = Loader.loadPDF(bytes)) {
            int pageCount = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            StringBuilder fullText = new StringBuilder();
            List<ParsedSection> sections = new ArrayList<>();

            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document).trim();

                if (!pageText.isEmpty()) {
                    fullText.append("--- Page ").append(page).append(" ---\n");
                    fullText.append(pageText).append("\n\n");

                    sections.add(new ParsedSection(
                            "Page " + page,
                            pageText,
                            page
                    ));
                }
            }

            String docTitle = originalFilename;
            if (document.getDocumentInformation() != null && document.getDocumentInformation().getTitle() != null) {
                String metaTitle = document.getDocumentInformation().getTitle().trim();
                if (!metaTitle.isEmpty()) {
                    docTitle = metaTitle;
                }
            }

            ParsedDocument parsedDoc = new ParsedDocument(docTitle, fullText.toString().trim(), pageCount, sections);
            if (document.getDocumentInformation() != null) {
                if (document.getDocumentInformation().getAuthor() != null) {
                    parsedDoc.getMetadata().put("author", document.getDocumentInformation().getAuthor());
                }
                if (document.getDocumentInformation().getSubject() != null) {
                    parsedDoc.getMetadata().put("subject", document.getDocumentInformation().getSubject());
                }
            }
            return parsedDoc;
        }
    }
}
