package com.knowledgehub.okf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OkfFrontmatter {
    private String id;
    private String type = "document"; // Required non-empty string in OKF v0.2
    private String title;
    private String description;
    private String version = "0.2";
    private List<String> tags = new ArrayList<>();
    private List<Map<String, Object>> sources = new ArrayList<>();
    private Map<String, Object> provenance = new HashMap<>();
    private String createdAt;
    private String updatedAt;

    public OkfFrontmatter() {}

    public OkfFrontmatter(String id, String type, String title, String description) {
        this.id = id;
        this.type = type != null && !type.isBlank() ? type : "document";
        this.title = title;
        this.description = description;
        this.version = "0.2";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags != null ? tags : new ArrayList<>(); }

    public List<Map<String, Object>> getSources() { return sources; }
    public void setSources(List<Map<String, Object>> sources) { this.sources = sources != null ? sources : new ArrayList<>(); }

    public Map<String, Object> getProvenance() { return provenance; }
    public void setProvenance(Map<String, Object> provenance) { this.provenance = provenance != null ? provenance : new HashMap<>(); }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
