package com.knowledgehub.parser;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CsvDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String fileExtension) {
        return "csv".equalsIgnoreCase(fileExtension);
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String originalFilename) throws IOException {
        StringBuilder fullText = new StringBuilder();
        List<ParsedSection> sections = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();

            Iterable<CSVRecord> records = format.parse(reader);
            int rowNumber = 1;
            StringBuilder sectionBatch = new StringBuilder();
            int batchSize = 25;

            for (CSVRecord record : records) {
                Map<String, String> rowMap = record.toMap();
                StringBuilder rowText = new StringBuilder("Row ").append(rowNumber).append(": ");

                rowMap.forEach((col, val) -> {
                    if (val != null && !val.isBlank()) {
                        rowText.append(col).append(" = ").append(val).append("; ");
                    }
                });
                rowText.append("\n");

                fullText.append(rowText);
                sectionBatch.append(rowText);

                if (rowNumber % batchSize == 0) {
                    sections.add(new ParsedSection(
                            "Rows " + (rowNumber - batchSize + 1) + " to " + rowNumber,
                            sectionBatch.toString().trim(),
                            (rowNumber / batchSize)
                    ));
                    sectionBatch = new StringBuilder();
                }
                rowNumber++;
            }

            if (sectionBatch.length() > 0) {
                int start = ((rowNumber - 1) / batchSize) * batchSize + 1;
                sections.add(new ParsedSection(
                        "Rows " + start + " to " + (rowNumber - 1),
                        sectionBatch.toString().trim(),
                        ((rowNumber - 1) / batchSize) + 1
                ));
            }
        }

        if (sections.isEmpty() && fullText.length() > 0) {
            sections.add(new ParsedSection("CSV Data", fullText.toString().trim(), 1));
        }

        return new ParsedDocument(originalFilename, fullText.toString().trim(), Math.max(1, sections.size()), sections);
    }
}
