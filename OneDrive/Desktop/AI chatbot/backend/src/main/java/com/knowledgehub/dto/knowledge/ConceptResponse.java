package com.knowledgehub.dto.knowledge;

import java.util.List;
import java.util.Map;

public class ConceptResponse {
    private String id;
    private String type;
    private String title;
    private String description;
    private List<String> tags;
    private List<Map<String, Object>> sources;
    private String rawMarkdown;

    public ConceptResponse() {}

    public ConceptResponse(String id, String type, String title, String description, List<String> tags,
                           List<Map<String, Object>> sources, String rawMarkdown) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.sources = sources;
        this.rawMarkdown = rawMarkdown;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<Map<String, Object>> getSources() { return sources; }
    public void setSources(List<Map<String, Object>> sources) { this.sources = sources; }

    public String getRawMarkdown() { return rawMarkdown; }
    public void setRawMarkdown(String rawMarkdown) { this.rawMarkdown = rawMarkdown; }
}
