package com.knowledgehub.parser;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentParser {
    boolean supports(String fileExtension);
    ParsedDocument parse(InputStream inputStream, String originalFilename) throws IOException;
}
