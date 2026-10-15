# KnowledgeHub AI - Architecture & System Design

KnowledgeHub AI is an enterprise knowledge assistant application designed for secure, self-hosted document ingestion, Open Knowledge Format (OKF v0.2) compliance, vector similarity search, and local Ollama RAG generation without reliance on external paid LLM APIs.

---

## 1. Overall System Architecture

```mermaid
graph TD
    Client[Browser / React 18 SPA] <-->|HTTPS REST + JWT| Gateway[Spring Boot Web Layer]
    
    subgraph Backend [KnowledgeHub Core Services]
        Gateway --> AuthCtrl[Authentication & RBAC Controller]
        Gateway --> DocCtrl[Document Management Controller]
        Gateway --> ChatCtrl[Chat & RAG Controller]
        Gateway --> OkfCtrl[OKF Knowledge Controller]
        Gateway --> SysCtrl[System & Health Controller]
        
        DocCtrl --> IngestEngine[Ingestion Pipeline Engine]
        ChatCtrl --> RagEngine[RAG Synthesis Service]
        OkfCtrl --> OkfEngine[OKF v0.2 Serializer & Validator]
        
        IngestEngine --> ParserFactory[Document Parser Factory]
        IngestEngine --> OkfEngine
        IngestEngine --> Chunker[Sliding Window Text Chunker]
        IngestEngine --> OllamaClient[Ollama Client]
        
        RagEngine --> VectorSearch[Vector Cosine & Hybrid Search]
        RagEngine --> OllamaClient
    end
    
    subgraph Storage [Persistence & AI Infrastructure]
        Postgres[(PostgreSQL 16 + pgvector)]
        FileStore[(Local Secure File System & OKF Bundles)]
        OllamaEngine[Local Ollama Inference Service]
    end
    
    IngestEngine -->|Save Chunks & Vectors| Postgres
    IngestEngine -->|Store Original & OKF| FileStore
    VectorSearch -->|Retrieve Chunks| Postgres
    OllamaClient -->|Chat / Embeddings| OllamaEngine
```

---

## 2. Document Ingestion Pipeline

```mermaid
sequenceDiagram
    autonumber
    actor User as Authorized User
    participant Controller as DocumentController
    participant Ingestion as IngestionService
    participant Parser as DocumentParserFactory
    participant OKF as OkfService
    participant Chunker as TextChunker
    participant Ollama as OllamaClient
    participant DB as PostgreSQL (pgvector)

    User->>Controller: Upload File (PDF/DOCX/TXT/MD/CSV)
    Controller->>Controller: Validate MIME, extension, size limit (25MB)
    Controller->>Ingestion: processDocumentAsync(docId)
    Controller-->>User: 201 Created (Status: PENDING)

    Note over Ingestion: Stage 1: EXTRACTING
    Ingestion->>Parser: Parse File Stream
    Parser-->>Ingestion: ParsedDocument (Sections, Pages, Metadata)

    Note over Ingestion: Stage 2: CONVERTING_OKF
    Ingestion->>OKF: generateFromParsed(doc, parsedDoc)
    OKF->>OKF: Validate YAML frontmatter (OKF v0.2)
    OKF-->>Ingestion: Stored doc-{id}.okf.md

    Note over Ingestion: Stage 3: CHUNKING
    Ingestion->>Chunker: chunkDocument(size: 800, overlap: 150)
    Chunker-->>Ingestion: List<TextChunk>

    Note over Ingestion: Stage 4: EMBEDDING
    Ingestion->>Ollama: Batch generateEmbedding(chunkText)
    Ollama-->>Ingestion: Vector List (e.g. 768 dims)

    Ingestion->>DB: Save DocumentChunks with Embeddings
    Ingestion->>DB: Update Status = COMPLETED
    Note over User,DB: Document is now indexed and searchable in chat!
```

---

## 3. RAG Query Flow

```mermaid
sequenceDiagram
    autonumber
    actor User as User
    participant Chat as ChatController
    participant RAG as RagService
    participant Search as VectorSearchService
    participant Ollama as OllamaClient
    participant DB as DocumentChunkRepository

    User->>Chat: POST /api/v1/chat/sessions/{id}/messages
    Chat->>DB: Save USER ChatMessage
    Chat->>RAG: query(userQuestion, userId, history)
    
    RAG->>Ollama: generateEmbedding(userQuestion)
    Ollama-->>RAG: queryVector
    
    RAG->>Search: search(queryVector, question, userId, topK, threshold)
    Search->>DB: Fetch user document chunks
    Search->>Search: Compute Cosine Similarity & Hybrid Boost
    Search->>Search: Deduplicate and filter chunks >= threshold
    Search-->>RAG: List<RetrievalResult> (chunks with doc title, page, section)
    
    alt If evidence is insufficient
        RAG-->>Chat: "The available documents do not provide enough information." (0 citations)
    else Grounded evidence found
        RAG->>RAG: Construct prompt with Context & Injection Shield
        RAG->>Ollama: generateAnswer(systemPrompt, userPrompt, history)
        Ollama-->>RAG: answer text
        RAG-->>Chat: RagResponse (Answer + Structured Citations)
    end
    
    Chat->>DB: Save ASSISTANT ChatMessage with citations
    Chat-->>User: 200 OK (Message with Citation Cards)
```

---

## 4. Database Relationships (ER Diagram)

```mermaid
erDiagram
    USERS ||--o{ KNOWLEDGE_DOCUMENTS : owns
    USERS ||--o{ CHAT_SESSIONS : owns
    USERS ||--o{ AUDIT_EVENTS : triggers
    KNOWLEDGE_DOCUMENTS ||--o{ DOCUMENT_PROCESSING_JOBS : tracks
    KNOWLEDGE_DOCUMENTS ||--o{ DOCUMENT_CHUNKS : contains
    CHAT_SESSIONS ||--o{ CHAT_MESSAGES : contains

    USERS {
        string id PK
        string username UK
        string email UK
        string password_hash
        string full_name
        string role
        boolean enabled
        timestamp created_at
    }

    KNOWLEDGE_DOCUMENTS {
        string id PK
        string user_id FK
        string title
        string original_filename
        string stored_filename
        string file_type
        bigint file_size
        string status
        text error_message
        string okf_path
        int chunk_count
        int page_count
        timestamp created_at
    }

    DOCUMENT_PROCESSING_JOBS {
        string id PK
        string document_id FK
        string status
        string stage
        text error_message
        timestamp started_at
        timestamp completed_at
    }

    DOCUMENT_CHUNKS {
        string id PK
        string document_id FK
        string user_id FK
        int chunk_index
        text content
        int token_estimate
        int page_number
        string section_name
        string embedding_model
        int embedding_dimensions
        text embedding_vector_json
        timestamp created_at
    }

    CHAT_SESSIONS {
        string id PK
        string user_id FK
        string title
        timestamp created_at
        timestamp updated_at
    }

    CHAT_MESSAGES {
        string id PK
        string session_id FK
        string role
        text content
        text citations_json
        timestamp created_at
    }

    AUDIT_EVENTS {
        string id PK
        string user_id
        string action
        string resource_type
        string resource_id
        text details
        string ip_address
        timestamp created_at
    }
```

---

## 5. Deployment Topology

```mermaid
graph TD
    subgraph DockerHost [Docker Compose Environment]
        NGINX[Frontend Container (Port 3000 / Nginx)]
        SpringBoot[Backend Container (Port 8080 / Temurin 17 JRE)]
        PGVector[Database Container (Port 5432 / pgvector:pg16)]
        
        NGINX -->|Reverse Proxy /api| SpringBoot
        SpringBoot -->|JDBC Connection| PGVector
    end
    
    subgraph LocalAI [Local Host Machine]
        HostOllama[Ollama Inference Engine (Port 11434)]
    end
    
    SpringBoot -->|HTTP API calls / host.docker.internal| HostOllama
    User([User Browser]) -->|HTTP :3000| NGINX
```
