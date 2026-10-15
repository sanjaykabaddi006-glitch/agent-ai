package com.knowledgehub.okf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.knowledgehub.common.exception.BadRequestException;
import com.knowledgehub.common.exception.ResourceNotFoundException;
import com.knowledgehub.config.KnowledgeHubProperties;
import com.knowledgehub.domain.document.KnowledgeDocument;
import com.knowledgehub.domain.document.KnowledgeDocumentRepository;
import com.knowledgehub.parser.ParsedDocument;
import com.knowledgehub.parser.ParsedSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class OkfService {

    private static final Logger log = LoggerFactory.getLogger(OkfService.class);
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("^---\\r?\\n(.*?)\\r?\\n---\\r?\\n?(.*)$", Pattern.DOTALL);

    private final KnowledgeHubProperties properties;
    private final OkfValidator okfValidator;
    private final KnowledgeDocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    public OkfService(KnowledgeHubProperties properties,
                      OkfValidator okfValidator,
                      KnowledgeDocumentRepository documentRepository) {
        this.properties = properties;
        this.okfValidator = okfValidator;
        this.documentRepository = documentRepository;
        this.objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public OkfDocument generateFromParsed(KnowledgeDocument doc, ParsedDocument parsedDoc) {
        String now = Instant.now().toString();

        OkfFrontmatter frontmatter = new OkfFrontmatter();
        frontmatter.setId(doc.getId());
        frontmatter.setType("document");
        frontmatter.setTitle(doc.getTitle() != null && !doc.getTitle().isBlank() ? doc.getTitle() : doc.getOriginalFilename());

        // Extract a concise description from the first 250 characters
        String sampleText = parsedDoc.getFullText() != null ? parsedDoc.getFullText().replaceAll("\\s+", " ").trim() : "";
        if (sampleText.length() > 250) {
            sampleText = sampleText.substring(0, 247) + "...";
        }
        frontmatter.setDescription(sampleText.isEmpty() ? "Business document ingested into KnowledgeHub AI." : sampleText);
        frontmatter.setVersion("0.2");

        // Tags
        List<String> tags = new ArrayList<>();
        tags.add(doc.getFileType().toLowerCase());
        tags.add("business-knowledge");
        frontmatter.setTags(tags);

        // Sources
        Map<String, Object> sourceMap = new LinkedHashMap<>();
        sourceMap.put("name", doc.getOriginalFilename());
        sourceMap.put("type", doc.getFileType());
        sourceMap.put("size_bytes", doc.getFileSize());
        sourceMap.put("pages", doc.getPageCount());
        frontmatter.setSources(Collections.singletonList(sourceMap));

        // Provenance
        Map<String, Object> provenance = new LinkedHashMap<>();
        provenance.put("system", "KnowledgeHub AI");
        provenance.put("format_version", "0.2");
        provenance.put("ingested_at", now);
        frontmatter.setProvenance(provenance);

        frontmatter.setCreatedAt(now);
        frontmatter.setUpdatedAt(now);

        // Construct Markdown Body
        StringBuilder body = new StringBuilder();
        body.append("# ").append(frontmatter.getTitle()).append("\n\n");
        body.append("> Source Document: `").append(doc.getOriginalFilename()).append("` | Type: `").append(doc.getFileType().toUpperCase()).append("`\n\n");

        if (parsedDoc.getSections() != null && !parsedDoc.getSections().isEmpty()) {
            for (ParsedSection section : parsedDoc.getSections()) {
                body.append("## ").append(section.getTitle()).append("\n\n");
                if (section.getPageNumber() != null) {
                    body.append("*Page: ").append(section.getPageNumber()).append("*\n\n");
                }
                body.append(section.getContent()).append("\n\n");
            }
        } else if (parsedDoc.getFullText() != null) {
            body.append(parsedDoc.getFullText()).append("\n\n");
        }

        String serialized = serializeToMarkdown(frontmatter, body.toString());
        return new OkfDocument(frontmatter, body.toString(), serialized);
    }

    public String serializeToMarkdown(OkfFrontmatter frontmatter, String markdownBody) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", frontmatter.getId());
        map.put("type", frontmatter.getType());
        map.put("title", frontmatter.getTitle());
        map.put("description", frontmatter.getDescription());
        map.put("version", frontmatter.getVersion());
        map.put("tags", frontmatter.getTags());
        map.put("sources", frontmatter.getSources());
        map.put("provenance", frontmatter.getProvenance());
        map.put("created_at", frontmatter.getCreatedAt());
        map.put("updated_at", frontmatter.getUpdatedAt());

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setAllowUnicode(true);
        options.setIndent(2);

        Representer representer = new Representer(new DumperOptions());
        Yaml yaml = new Yaml(representer, options);

        String yamlString = yaml.dump(map).trim();
        return "---\n" + yamlString + "\n---\n\n" + (markdownBody != null ? markdownBody.trim() : "");
    }

    public String saveOkfFile(String docId, String content) throws IOException {
        String safeName = sanitizeFilename("doc-" + docId + ".okf.md");
        Path okfDirPath = Paths.get(properties.getStorage().getOkfDir()).toAbsolutePath().normalize();
        Files.createDirectories(okfDirPath);

        Path targetPath = okfDirPath.resolve(safeName).normalize();
        if (!targetPath.startsWith(okfDirPath)) {
            throw new BadRequestException("Invalid path traversal attempt");
        }

        Files.writeString(targetPath, content, StandardCharsets.UTF_8);
        return targetPath.toString();
    }

    public OkfDocument readOkfDocument(String docId, String userId) {
        KnowledgeDocument doc = documentRepository.findByIdAndUserId(docId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found or access denied: " + docId));

        if (doc.getOkfPath() == null) {
            throw new ResourceNotFoundException("OKF representation not generated yet for document: " + docId);
        }

        Path path = Paths.get(doc.getOkfPath());
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("OKF file missing from storage: " + doc.getOkfPath());
        }

        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            return parseRawOkf(raw);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read OKF file from storage", e);
        }
    }

    public OkfDocument parseRawOkf(String rawMarkdown) {
        Matcher matcher = FRONTMATTER_PATTERN.matcher(rawMarkdown.trim());
        if (!matcher.find()) {
            throw new BadRequestException("Not a valid OKF document: missing frontmatter");
        }

        String yamlContent = matcher.group(1);
        String bodyContent = matcher.group(2);

        LoaderOptions loaderOptions = new LoaderOptions();
        Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) yaml.load(yamlContent);

        OkfFrontmatter fm = new OkfFrontmatter();
        if (map.get("id") != null) fm.setId(map.get("id").toString());
        if (map.get("type") != null) fm.setType(map.get("type").toString());
        if (map.get("title") != null) fm.setTitle(map.get("title").toString());
        if (map.get("description") != null) fm.setDescription(map.get("description").toString());
        if (map.get("version") != null) fm.setVersion(map.get("version").toString());

        if (map.get("tags") instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) map.get("tags");
            fm.setTags(tags);
        }
        if (map.get("sources") instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sources = (List<Map<String, Object>>) map.get("sources");
            fm.setSources(sources);
        }
        if (map.get("provenance") instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> prov = (Map<String, Object>) map.get("provenance");
            fm.setProvenance(prov);
        }
        if (map.get("created_at") != null) fm.setCreatedAt(map.get("created_at").toString());
        if (map.get("updated_at") != null) fm.setUpdatedAt(map.get("updated_at").toString());

        return new OkfDocument(fm, bodyContent != null ? bodyContent.trim() : "", rawMarkdown);
    }

    public byte[] exportBundleZip(String userId) throws IOException {
        List<KnowledgeDocument> docs = documentRepository.findByUserIdOrderByCreatedAtDesc(userId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            OkfBundle bundle = new OkfBundle(UUID.randomUUID().toString(), "KnowledgeHub OKF Export Bundle");
            bundle.setDocumentCount(docs.size());

            // Add each OKF document to the zip
            for (KnowledgeDocument doc : docs) {
                if (doc.getOkfPath() != null) {
                    Path path = Paths.get(doc.getOkfPath());
                    if (Files.exists(path)) {
                        String content = Files.readString(path, StandardCharsets.UTF_8);
                        String zipFilename = "concepts/" + sanitizeFilename(doc.getTitle()) + "-" + doc.getId() + ".okf.md";

                        ZipEntry entry = new ZipEntry(zipFilename);
                        zos.putNextEntry(entry);
                        zos.write(content.getBytes(StandardCharsets.UTF_8));
                        zos.closeEntry();

                        bundle.getConcepts().add(new OkfBundle.OkfConceptSummary(
                                doc.getId(),
                                "document",
                                doc.getTitle(),
                                zipFilename,
                                List.of(doc.getFileType().toLowerCase())
                        ));
                    }
                }
            }

            // Add Root Index / manifest.json
            ZipEntry indexEntry = new ZipEntry("index.json");
            zos.putNextEntry(indexEntry);
            zos.write(objectMapper.writeValueAsBytes(bundle));
            zos.closeEntry();

            // Add Bundle README
            String readme = "# Open Knowledge Format (OKF v0.2) Bundle\n\n" +
                    "Exported from KnowledgeHub AI.\n" +
                    "Format Specification: https://github.com/GoogleCloudPlatform/open-knowledge-format\n\n" +
                    "Total Concepts/Documents: " + bundle.getDocumentCount() + "\n" +
                    "Bundle ID: " + bundle.getBundleId() + "\n" +
                    "Generated: " + bundle.getGeneratedAt() + "\n";

            ZipEntry readmeEntry = new ZipEntry("README.md");
            zos.putNextEntry(readmeEntry);
            zos.write(readme.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        return baos.toByteArray();
    }

    public void deleteOkfFile(String okfPath) {
        if (okfPath != null) {
            try {
                Path path = Paths.get(okfPath);
                Files.deleteIfExists(path);
            } catch (Exception e) {
                log.warn("Failed to delete OKF file at {}: {}", okfPath, e.getMessage());
            }
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "doc.okf.md";
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}
