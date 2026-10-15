package com.knowledgehub.parser;

public class ParsedSection {
    private String title;
    private String content;
    private Integer pageNumber;

    public ParsedSection() {}

    public ParsedSection(String title, String content, Integer pageNumber) {
        this.title = title;
        this.content = content;
        this.pageNumber = pageNumber;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
}
