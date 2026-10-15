package com.knowledgehub.rag;

public class TextChunk {
    private int index;
    private String content;
    private Integer pageNumber;
    private String sectionName;
    private int tokenEstimate;

    public TextChunk() {}

    public TextChunk(int index, String content, Integer pageNumber, String sectionName) {
        this.index = index;
        this.content = content;
        this.pageNumber = pageNumber;
        this.sectionName = sectionName;
        this.tokenEstimate = content != null ? Math.max(1, content.split("\\s+").length) : 0;
    }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public int getTokenEstimate() { return tokenEstimate; }
    public void setTokenEstimate(int tokenEstimate) { this.tokenEstimate = tokenEstimate; }
}
