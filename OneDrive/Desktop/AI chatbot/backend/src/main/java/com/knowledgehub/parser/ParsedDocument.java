package com.knowledgehub.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParsedDocument {
    private String title;
    private String fullText;
    private int pageCount = 1;
    private List<ParsedSection> sections = new ArrayList<>();
    private Map<String, Object> metadata = new HashMap<>();

    public ParsedDocument() {}

    public ParsedDocument(String title, String fullText, int pageCount, List<ParsedSection> sections) {
        this.title = title;
        this.fullText = fullText;
        this.pageCount = Math.max(1, pageCount);
        this.sections = sections != null ? sections : new ArrayList<>();
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFullText() { return fullText; }
    public void setFullText(String fullText) { this.fullText = fullText; }

    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = Math.max(1, pageCount); }

    public List<ParsedSection> getSections() { return sections; }
    public void setSections(List<ParsedSection> sections) { this.sections = sections; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
