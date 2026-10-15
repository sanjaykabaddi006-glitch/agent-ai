# KnowledgeHub AI - Database Architecture & Migrations

KnowledgeHub AI uses PostgreSQL with the **pgvector** extension for relational metadata storage, document processing workflows, chat history, and vector similarity search.

---

## 1. Migration Strategy (Flyway)

Database migrations are versioned under `backend/src/main/resources/db/migration`:
- `V1__init_schema.sql`: Enables `uuid-ossp` and `vector` extensions; provisions tables `users`, `knowledge_documents`, `document_processing_jobs`, `document_chunks`, `chat_sessions`, `chat_messages`, `audit_events` with indexes.
- `V2__seed_admin.sql`: Seeds initial administrative and demo credentials (`admin` / `Admin123!` and `demo` / `Demo123!`).

---

## 2. Table Specifications

### 2.1 `users`
- Stores user credentials and role assignments (`ROLE_USER`, `ROLE_ADMIN`).
- Password hashes generated via standard BCrypt (`strength=10`).
- Unique constraints on `username` and `email`.

### 2.2 `knowledge_documents`
- Primary tracking record for business files.
- Columns: `id`, `user_id`, `title`, `original_filename`, `stored_filename`, `file_type`, `file_size`, `status`, `error_message`, `okf_path`, `chunk_count`, `page_count`, `created_at`, `updated_at`.
- Status values: `PENDING`, `EXTRACTING`, `CONVERTING_OKF`, `CHUNKING`, `EMBEDDING`, `COMPLETED`, `FAILED`.

### 2.3 `document_processing_jobs`
- Stage-by-stage audit and observability tracking for document ingestion.
- Stages: `EXTRACTING`, `CONVERTING_OKF`, `CHUNKING`, `EMBEDDING`, `COMPLETED`, `FAILED`.

### 2.4 `document_chunks`
- Individual overlapping chunks extracted from documents.
- Includes `page_number` and `section_name` for accurate citation grounding.
- Embeddings stored with `embedding_dimensions` and model name (`nomic-embed-text`).

### 2.5 `chat_sessions` & `chat_messages`
- Manages multi-turn conversation sessions.
- `chat_messages` stores message role (`USER`, `ASSISTANT`, `SYSTEM`), textual content, and JSON-serialized structured `citations_json`.

### 2.6 `audit_events`
- Immutable audit log of administrative and user actions (`LOGIN`, `REGISTER`, `UPLOAD`, `RETRY`, `DELETE`, `CHAT`).

---

## 3. Vector Similarity & pgvector Configuration

When deploying on PostgreSQL with pgvector:
1. Extension is created automatically:
   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   ```
2. Similarity search can utilize cosine distance:
   ```sql
   SELECT chunk_index, content, 1 - (embedding_vector <=> query_vector) AS cosine_similarity
   FROM document_chunks
   WHERE user_id = :userId
   ORDER BY embedding_vector <=> query_vector ASC
   LIMIT 5;
   ```
3. For large-scale datasets (>10,000 vectors), an HNSW index can be applied:
   ```sql
   CREATE INDEX idx_chunks_hnsw ON document_chunks 
   USING hnsw (embedding_vector vector_cosine_ops);
   ```

---

## 4. Backup and Restore

### Backup
```bash
docker exec -t knowledgehub-postgres pg_dump -U postgres knowledgehub > backup_$(date +%Y%m%d).sql
```

### Restore
```bash
docker exec -i knowledgehub-postgres psql -U postgres -d knowledgehub < backup_20261010.sql
```
