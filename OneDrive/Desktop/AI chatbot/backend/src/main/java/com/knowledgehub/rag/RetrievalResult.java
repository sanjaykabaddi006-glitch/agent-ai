package com.knowledgehub.rag;

public class RetrievalResult {
    private String chunkId;
    private String documentId;
    private String documentTitle;
    private String content;
    private Integer pageNumber;
    private String sectionName;
    private int chunkIndex;
    private double score;

    public RetrievalResult() {}

    public RetrievalResult(String chunkId, String documentId, String documentTitle, String content,
                           Integer pageNumber, String sectionName, int chunkIndex, double score) {
        this.chunkId = chunkId;
        this.documentId = documentId;
        this.documentTitle = documentTitle;
        this.content = content;
        this.pageNumber = pageNumber;
        this.sectionName = sectionName;
        this.chunkIndex = chunkIndex;
        this.score = score;
    }

    public Citation toCitation() {
        String snippet = content != null && content.length() > 200 ? content.substring(0, 197) + "..." : content;
        return new Citation(documentId, documentTitle, pageNumber, sectionName, chunkIndex, snippet, score);
    }

    public String getChunkId() { return chunkId; }
    public void setChunkId(String chunkId) { this.chunkId = chunkId; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
}
