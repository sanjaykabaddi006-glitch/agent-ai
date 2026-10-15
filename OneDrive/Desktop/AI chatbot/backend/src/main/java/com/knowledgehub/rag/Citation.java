package com.knowledgehub.rag;

public class Citation {
    private String documentId;
    private String documentTitle;
    private Integer pageNumber;
    private String sectionName;
    private int chunkIndex;
    private String snippet;
    private double similarityScore;

    public Citation() {}

    public Citation(String documentId, String documentTitle, Integer pageNumber, String sectionName, int chunkIndex, String snippet, double similarityScore) {
        this.documentId = documentId;
        this.documentTitle = documentTitle;
        this.pageNumber = pageNumber;
        this.sectionName = sectionName;
        this.chunkIndex = chunkIndex;
        this.snippet = snippet;
        this.similarityScore = similarityScore;
    }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }

    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }

    public double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(double similarityScore) { this.similarityScore = similarityScore; }
}
