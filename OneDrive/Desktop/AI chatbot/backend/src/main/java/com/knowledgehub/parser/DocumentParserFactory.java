package com.knowledgehub.parser;

import com.knowledgehub.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocumentParserFactory {

    private final List<DocumentParser> parsers;

    public DocumentParserFactory(List<DocumentParser> parsers) {
        this.parsers = parsers;
    }

    public DocumentParser getParser(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BadRequestException("File must have a valid extension: " + filename);
        }

        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        return parsers.stream()
                .filter(p -> p.supports(extension))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Unsupported file type: ." + extension +
                        ". Supported formats are: PDF, DOCX, PPT, PPTX, TXT, MD, CSV"));
    }
}
