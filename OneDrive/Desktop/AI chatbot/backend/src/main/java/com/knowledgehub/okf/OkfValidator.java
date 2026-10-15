package com.knowledgehub.okf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OkfValidator {

    private static final Logger log = LoggerFactory.getLogger(OkfValidator.class);
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile("^---\\r?\\n(.*?)\\r?\\n---\\r?\\n?(.*)$", Pattern.DOTALL);

    public OkfValidationResult validate(String rawMarkdown) {
        OkfValidationResult result = new OkfValidationResult();

        if (rawMarkdown == null || rawMarkdown.trim().isEmpty()) {
            result.addError("OKF document content cannot be empty");
            return result;
        }

        Matcher matcher = FRONTMATTER_PATTERN.matcher(rawMarkdown.trim());
        if (!matcher.find()) {
            result.addError("OKF document must begin with valid YAML frontmatter bounded by '---' markers");
            return result;
        }

        String yamlContent = matcher.group(1);
        String bodyContent = matcher.group(2);

        if (yamlContent == null || yamlContent.trim().isEmpty()) {
            result.addError("Frontmatter YAML block cannot be empty");
            return result;
        }

        // Parse YAML safely
        Map<String, Object> frontmatterMap;
        try {
            LoaderOptions loaderOptions = new LoaderOptions();
            loaderOptions.setMaxAliasesForCollections(50);
            Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));
            Object loaded = yaml.load(yamlContent);
            if (!(loaded instanceof Map)) {
                result.addError("Frontmatter must be a valid YAML key-value mapping");
                return result;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> casted = (Map<String, Object>) loaded;
            frontmatterMap = casted;
        } catch (Exception e) {
            result.addError("Malformed YAML in frontmatter: " + e.getMessage());
            return result;
        }

        // 1. Required 'type' field: non-empty string in OKF v0.2
        Object typeObj = frontmatterMap.get("type");
        if (typeObj == null || !(typeObj instanceof String) || ((String) typeObj).trim().isEmpty()) {
            result.addError("Required OKF field 'type' is missing, empty, or not a string");
        }

        // 2. Required 'title' field
        Object titleObj = frontmatterMap.get("title");
        if (titleObj == null || !(titleObj instanceof String) || ((String) titleObj).trim().isEmpty()) {
            result.addError("Required OKF field 'title' is missing, empty, or not a string");
        }

        // 3. Identifier
        Object idObj = frontmatterMap.get("id");
        if (idObj == null || idObj.toString().trim().isEmpty()) {
            result.addWarning("OKF field 'id' is recommended for tracking concept identity");
        }

        // 4. Body Content
        if (bodyContent == null || bodyContent.trim().isEmpty()) {
            result.addWarning("OKF Markdown body is empty");
        }

        return result;
    }

    public OkfValidationResult validate(OkfDocument document) {
        if (document == null) {
            OkfValidationResult res = new OkfValidationResult();
            res.addError("Document cannot be null");
            return res;
        }
        if (document.getRawContent() != null && !document.getRawContent().isEmpty()) {
            return validate(document.getRawContent());
        }

        OkfValidationResult result = new OkfValidationResult();
        OkfFrontmatter fm = document.getFrontmatter();
        if (fm == null) {
            result.addError("Frontmatter cannot be null");
            return result;
        }

        if (fm.getType() == null || fm.getType().trim().isEmpty()) {
            result.addError("Required field 'type' must be non-empty");
        }
        if (fm.getTitle() == null || fm.getTitle().trim().isEmpty()) {
            result.addError("Required field 'title' must be non-empty");
        }
        if (document.getMarkdownBody() == null || document.getMarkdownBody().trim().isEmpty()) {
            result.addWarning("Markdown body is empty");
        }

        return result;
    }
}
