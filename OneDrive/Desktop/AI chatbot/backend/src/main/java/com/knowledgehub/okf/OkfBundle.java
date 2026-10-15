package com.knowledgehub.okf;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OkfBundle {
    private String bundleId;
    private String name;
    private String version = "0.2";
    private Instant generatedAt;
    private int documentCount;
    private List<OkfConceptSummary> concepts = new ArrayList<>();

    public OkfBundle() {
        this.generatedAt = Instant.now();
    }

    public OkfBundle(String bundleId, String name) {
        this.bundleId = bundleId;
        this.name = name;
        this.version = "0.2";
        this.generatedAt = Instant.now();
    }

    public static class OkfConceptSummary {
        private String id;
        private String type;
        private String title;
        private String filename;
        private List<String> tags = new ArrayList<>();

        public OkfConceptSummary() {}

        public OkfConceptSummary(String id, String type, String title, String filename, List<String> tags) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.filename = filename;
            this.tags = tags != null ? tags : new ArrayList<>();
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }

    public String getBundleId() { return bundleId; }
    public void setBundleId(String bundleId) { this.bundleId = bundleId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }

    public int getDocumentCount() { return documentCount; }
    public void setDocumentCount(int documentCount) { this.documentCount = documentCount; }

    public List<OkfConceptSummary> getConcepts() { return concepts; }
    public void setConcepts(List<OkfConceptSummary> concepts) { this.concepts = concepts; }
}
