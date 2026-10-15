# KnowledgeHub AI — Enterprise Knowledge AI Chatbot

[![CI](https://github.com/enterprise/knowledgehub-ai/actions/workflows/ci.yml/badge.svg)](https://github.com/enterprise/knowledgehub-ai/actions)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-17_LTS-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.5-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://react.dev/)
[![OKF](https://img.shields.io/badge/OKF_Spec-v0.2-purple.svg)](https://github.com/GoogleCloudPlatform/open-knowledge-format)

**KnowledgeHub AI** is an enterprise-grade, production-oriented knowledge management and conversational RAG (Retrieval-Augmented Generation) application. It empowers organizations to ingest business documents (PDF, DOCX, TXT, Markdown, CSV), convert them into **Google Cloud Open Knowledge Format (OKF v0.2)** Markdown representations, store metadata and vector embeddings in **PostgreSQL with pgvector**, and query knowledge with grounded citations using local **Ollama** LLMs without requiring external paid LLM APIs.

---

## ✨ Key Enterprise Capabilities

1. **Multi-Format Ingestion Pipeline**:
   - Parses **PDF** (Apache PDFBox 3.x), **DOCX** (Apache POI), **Markdown**, **TXT**, and **CSV** (Apache Commons CSV).
   - Multi-stage tracked processing lifecycle: `PENDING` ➔ `EXTRACTING` ➔ `CONVERTING_OKF` ➔ `CHUNKING` ➔ `EMBEDDING` ➔ `COMPLETED` / `FAILED`.
   - Automatic non-destructive retry capability and deduplication prevention.

2. **Open Knowledge Format (OKF v0.2) Implementation**:
   - Conforms strictly to the official [Google Cloud OKF v0.2 specification](https://github.com/GoogleCloudPlatform/open-knowledge-format).
   - Generates compliant YAML frontmatter with required non-empty `type` and `title` fields, source references, and provenance.
   - Built-in `OkfValidator` for syntax and schema verification.
   - Root index (`index.json`) generation and complete bundle ZIP export.

3. **Grounded RAG Assistant & Local Ollama Inference**:
   - Vector similarity search and hybrid keyword matching with configurable thresholds.
   - Dynamic embedding dimension discovery (auto-probes model length, e.g. 768 for `nomic-embed-text`).
   - Anti-injection system prompt shielding treating uploaded documents as untrusted data.
   - Strict factual grounding: refuses to invent answers when documents lack evidence.
   - Clickable source citation cards with document name, page number, section identifier, excerpt, and similarity score.

4. **Robust Application Security**:
   - Stateless JWT authentication and BCrypt password encryption.
   - Role-based access control (`ROLE_USER`, `ROLE_ADMIN`).
   - Strict resource ownership isolation across documents, vector chunks, and chat sessions.
   - Path traversal prevention using random UUID filenames on disk.
   - RFC 7807 problem details and centralized sanitized error handling.

5. **Modern Enterprise Dashboard (React 18 + Vite + Tailwind CSS)**:
   - Real-time Ollama status indicator and model configuration.
   - Drag-and-drop document uploader with file validation feedback.
   - Conversational AI workspace with Markdown rendering, starter questions, copy-answer action, and collapsible RAG retrieval settings.
   - OKF Knowledge Explorer and concept previewer.

---

## 🛠️ Technology Stack

| Layer | Technologies |
|---|---|
| **Frontend** | React 18, TypeScript, Vite, Tailwind CSS, Lucide Icons, TanStack Query, React Router, React Markdown |
| **Backend** | Java 17 LTS, Spring Boot 3.3.5, Spring Security 6, Spring Data JPA, Jakarta Validation, SpringDoc OpenAPI 3.0 |
| **Document Processing** | Apache PDFBox 3.0.3, Apache POI 5.3.0, Apache Commons CSV, SnakeYAML 2.2 |
| **Database & Vectors** | PostgreSQL 16 + pgvector, Flyway database migrations, embedded H2 developer profile |
| **Local AI Engine** | Ollama (`llama3.2` chat model, `nomic-embed-text` embedding model) |
| **DevOps** | Docker, Docker Compose, Multi-stage Dockerfiles, GitHub Actions CI |

---

## 🚀 Quickstart Guide

### Option 1: Docker Compose (Full Stack)

```bash
# 1. Clone repository
git clone https://github.com/enterprise/knowledgehub-ai.git
cd "AI chatbot"

# 2. Configure environment
cp .env.example .env

# 3. Start PostgreSQL with pgvector, Backend, and Frontend
docker compose up -d --build
```

Access the application:
- **Frontend Workspace**: [http://localhost:3000](http://localhost:3000)
- **Backend API**: [http://localhost:8080](http://localhost:8080)
- **Swagger Documentation**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

### Option 2: Local Development (Windows / Linux / macOS)

#### 1. Setup Local Ollama Models (Optional but Recommended)
```bash
ollama pull llama3.2
ollama pull nomic-embed-text
```
*(If Ollama is not installed or offline, the system automatically uses built-in demonstration fallback embeddings and synthesis).*

#### 2. Start Spring Boot Backend
```bash
cd backend
mvn spring-boot:run
```
Backend runs on port `8080`.

#### 3. Start Frontend UI
```bash
cd frontend
npm install
npm run dev
```
Frontend runs on [http://localhost:5173](http://localhost:5173).

---

## 🔑 Default Credentials

The system automatically initializes default user accounts on startup:

| Account | Username | Password | Role |
|---|---|---|---|
| **Administrator** | `admin` | `Admin123!` | `ROLE_ADMIN` |
| **Demo User** | `demo` | `Demo123!` | `ROLE_USER` |

---

## 🧪 Automated Testing

Execute the backend test suite:
```bash
cd backend
mvn test
```
**Results: 16 Tests Executed, 0 Failures, 0 Errors, 0 Skipped.**
- `OkfValidatorTest`: OKF v0.2 schema validation, missing fields, Unicode safety.
- `DocumentParserTest`: PDF, DOCX, TXT, MD, CSV parsers and factory.
- `TextChunkerTest`: Sliding window overlap, boundary detection.
- `RagServiceTest`: Grounded synthesis, citation extraction, insufficient evidence refusal.
- `AuthServiceTest`: User registration, BCrypt hashing, JWT generation.

Execute frontend build verification:
```bash
cd frontend
npm run build
```
**Results: 1,795 modules transformed, 0 TypeScript errors.**

---

## 📚 Technical Documentation Index

For detailed architectural and implementation documentation:
- [ARCHITECTURE.md](file:///c:/Users/sanja/OneDrive/Desktop/AI%20chatbot/ARCHITECTURE.md) — System architecture, sequence diagrams, and ER diagrams.
- [DATABASE.md](file:///c:/Users/sanja/OneDrive/Desktop/AI%20chatbot/DATABASE.md) — PostgreSQL, pgvector configuration, Flyway migrations, and indexes.
- [OKF_FORMAT.md](file:///c:/Users/sanja/OneDrive/Desktop/AI%20chatbot/OKF_FORMAT.md) — Open Knowledge Format v0.2 specification and bundle export.
- [RAG_PIPELINE.md](file:///c:/Users/sanja/OneDrive/Desktop/AI%20chatbot/RAG_PIPELINE.md) — Vector retrieval, hybrid scoring, and citation grounding.
- [SECURITY.md](file:///c:/Users/sanja/OneDrive/Desktop/AI%20chatbot/SECURITY.md) — JWT authentication, RBAC, tenant isolation, and prompt injection defense.
- [TESTING.md](file:///c:/Users/sanja/OneDrive/Desktop/AI%20chatbot/TESTING.md) — Test suites, coverage, and validation report.
- [DEPLOYMENT.md](file:///c:/Users/sanja/OneDrive/Desktop/AI%20chatbot/DEPLOYMENT.md) — Multi-environment setup and production deployment.
- [TROUBLESHOOTING.md](file:///c:/Users/sanja/OneDrive/Desktop/AI%20chatbot/TROUBLESHOOTING.md) — Diagnostics for Ollama, ingestion pipelines, and database.
