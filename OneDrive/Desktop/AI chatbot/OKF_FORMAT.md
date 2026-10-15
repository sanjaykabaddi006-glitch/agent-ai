# Open Knowledge Format (OKF v0.2) Implementation

KnowledgeHub AI adheres to the official **Open Knowledge Format (OKF) v0.2** specification:
[https://github.com/GoogleCloudPlatform/open-knowledge-format](https://github.com/GoogleCloudPlatform/open-knowledge-format)

---

## 1. What is OKF v0.2?

OKF is an open knowledge representation format that standardizes human-readable and machine-actionable knowledge into structured Markdown documents with validated YAML frontmatter.

---

## 2. Document Structure

Every generated OKF document consists of:
1. **YAML Frontmatter** (bounded by `---` markers)
2. **Markdown Body** (headings, sections, structured facts, tables, and notes)

### Example Generated OKF v0.2 Document:
```markdown
---
id: "doc-3f4a1234-abcd-5678"
type: "document"
title: "Enterprise Security Policy"
description: "Guidelines on multi-factor authentication and access controls."
version: "0.2"
tags:
  - "pdf"
  - "business-knowledge"
sources:
  - name: "SecurityPolicy_2026.pdf"
    type: "pdf"
    size_bytes: 489201
    pages: 14
provenance:
  system: "KnowledgeHub AI"
  format_version: "0.2"
  ingested_at: "2026-10-10T13:30:00Z"
created_at: "2026-10-10T13:30:00Z"
updated_at: "2026-10-10T13:30:00Z"
---

# Enterprise Security Policy

> Source Document: `SecurityPolicy_2026.pdf` | Type: `PDF`

## Section 1: Authentication Requirements

*Page: 1*

All internal staff must authenticate via multi-factor authentication (MFA) tokens.
```

---

## 3. Strict Compliance Rules Implemented

- **Required `type`**: Must be a non-empty string (e.g. `document`, `concept`, `procedure`).
- **Required `title`**: Must be a non-empty string derived from document metadata or primary heading.
- **Safe YAML Serialization**: SnakeYAML configured with `BLOCK` flow style and Unicode enablement (`allowUnicode: true`), avoiding unescaped quotes or invalid YAML.
- **Traceable Provenance**: Captures ingestion timestamps, pipeline version, and original source filenames without inventing fake metadata.
- **Separation of Concerns**: Original files are kept in `data/uploads/`, OKF markdown files are kept in `data/okf/`, and vector records are stored in PostgreSQL `document_chunks`.
- **Bundle Export & Root Index**:
  When exported, the system packages all concepts into a ZIP bundle containing:
  - `index.json`: Manifest index listing all concept IDs, types, titles, and paths.
  - `concepts/*.okf.md`: The individual OKF files.
  - `README.md`: Explaining the bundle metadata.
