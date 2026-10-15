# KnowledgeHub AI - Troubleshooting & Diagnostics

This guide provides operational solutions for common issues encountered during local development and deployment.

---

## 1. Ollama Connectivity & Model Issues

### Symptoms:
- Dashboard shows **Ollama: Offline (Demo Mode)**
- Chat responses indicate local demonstration fallback mode.

### Diagnosis & Resolution:
1. Check if the Ollama service is active:
   ```bash
   curl http://localhost:11434/api/tags
   ```
2. If Ollama is not running, start it:
   ```bash
   ollama serve
   ```
3. Verify that the configured models are downloaded:
   ```bash
   ollama list
   ```
   If `llama3.2` or `nomic-embed-text` are missing:
   ```bash
   ollama pull llama3.2
   ollama pull nomic-embed-text
   ```
4. Configure custom Ollama URL in `application.yml` or via environment variable:
   ```bash
   export OLLAMA_BASE_URL=http://your-ollama-host:11434
   ```

---

## 2. Document Ingestion Failures

### Symptoms:
- Document status shows `FAILED` in Document Library.

### Diagnosis & Resolution:
1. Open the document detail page (`/documents/{id}`) and click **Processing Pipeline Logs**.
2. Identify the failure stage:
   - **`EXTRACTING`**: Document may be password-protected or corrupt. Ensure PDF, DOCX, TXT, MD, or CSV is valid.
   - **`CONVERTING_OKF`**: Malformed character encodings. OKF serializer requires valid UTF-8.
   - **`EMBEDDING`**: Embedding dimension mismatch or timeout. Check Ollama responsiveness.
3. Click **Retry Ingestion** to re-run the pipeline cleanly. Old chunks and job records will be automatically pruned to avoid duplicate embeddings.

---

## 3. Database & pgvector Diagnostics

### Symptoms:
- Connection refused on `jdbc:postgresql://localhost:5432/knowledgehub`.

### Diagnosis & Resolution:
1. Ensure the PostgreSQL container is running:
   ```bash
   docker ps | grep postgres
   ```
2. If developing locally without a local PostgreSQL instance, run the backend with the default `dev` profile:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```
   The `dev` profile uses an embedded, zero-dependency database with full in-memory vector cosine similarity search.
3. To verify pgvector extension in PostgreSQL:
   ```sql
   SELECT * FROM pg_extension WHERE extname = 'vector';
   ```

---

## 4. Authentication & JWT Issues

### Symptoms:
- Requests return `401 Unauthorized` or redirect to login.

### Resolution:
1. The default JWT expiration is 24 hours (`86400000 ms`). Re-authenticate using the login page.
2. If custom JWT secrets are defined, ensure `JWT_SECRET` is identical across restarts.
