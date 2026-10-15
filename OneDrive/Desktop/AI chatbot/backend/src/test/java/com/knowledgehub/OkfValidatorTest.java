package com.knowledgehub;

import com.knowledgehub.okf.OkfDocument;
import com.knowledgehub.okf.OkfFrontmatter;
import com.knowledgehub.okf.OkfValidationResult;
import com.knowledgehub.okf.OkfValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OkfValidatorTest {

    private OkfValidator validator;

    @BeforeEach
    void setUp() {
        validator = new OkfValidator();
    }

    @Test
    @DisplayName("Should validate valid OKF v0.2 markdown successfully")
    void testValidOkf() {
        String markdown = """
                ---
                id: doc-101
                type: concept
                title: Enterprise Access Control
                description: Overview of Role-Based Access Control
                version: "0.2"
                tags:
                  - security
                  - enterprise
                ---
                
                # Enterprise Access Control
                
                RBAC manages user permissions based on enterprise roles.
                """;

        OkfValidationResult result = validator.validate(markdown);
        assertTrue(result.isValid(), "Document should be valid OKF v0.2");
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Should reject OKF document missing 'type' field")
    void testMissingTypeField() {
        String markdown = """
                ---
                id: doc-102
                title: Missing Type Field
                description: Document without required type
                ---
                
                # Content
                """;

        OkfValidationResult result = validator.validate(markdown);
        assertFalse(result.isValid(), "Document missing required type must fail validation");
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("type")));
    }

    @Test
    @DisplayName("Should reject OKF document missing 'title' field")
    void testMissingTitleField() {
        String markdown = """
                ---
                id: doc-103
                type: document
                ---
                
                # Content
                """;

        OkfValidationResult result = validator.validate(markdown);
        assertFalse(result.isValid(), "Document missing required title must fail validation");
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("title")));
    }

    @Test
    @DisplayName("Should reject malformed YAML frontmatter")
    void testMalformedYaml() {
        String markdown = """
                ---
                id: doc-104
                type: [unclosed list
                title: Broken YAML
                ---
                
                # Content
                """;

        OkfValidationResult result = validator.validate(markdown);
        assertFalse(result.isValid(), "Malformed YAML should fail validation");
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Malformed YAML")));
    }

    @Test
    @DisplayName("Should handle Unicode and special characters safely")
    void testUnicodeAndSpecialChars() {
        String markdown = """
                ---
                id: doc-unicode-✓
                type: document
                title: "Artificial Intelligence: ¿Cómo funciona? & 'Special: Quotes'"
                description: "Multilingual OKF document: 日本語, Español, Français, हिन्दी"
                tags:
                  - ai
                  - unicode-✓
                ---
                
                # Knowledge Document: 日本語
                Testing internationalization and Unicode compatibility in OKF v0.2.
                """;

        OkfValidationResult result = validator.validate(markdown);
        assertTrue(result.isValid(), "Unicode and special characters must be supported safely");
    }
}
