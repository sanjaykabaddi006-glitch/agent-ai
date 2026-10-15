package com.knowledgehub.okf;

public class OkfDocument {
    private OkfFrontmatter frontmatter;
    private String markdownBody;
    private String rawContent;

    public OkfDocument() {}

    public OkfDocument(OkfFrontmatter frontmatter, String markdownBody, String rawContent) {
        this.frontmatter = frontmatter;
        this.markdownBody = markdownBody;
        this.rawContent = rawContent;
    }

    public OkfFrontmatter getFrontmatter() { return frontmatter; }
    public void setFrontmatter(OkfFrontmatter frontmatter) { this.frontmatter = frontmatter; }

    public String getMarkdownBody() { return markdownBody; }
    public void setMarkdownBody(String markdownBody) { this.markdownBody = markdownBody; }

    public String getRawContent() { return rawContent; }
    public void setRawContent(String rawContent) { this.rawContent = rawContent; }
}
