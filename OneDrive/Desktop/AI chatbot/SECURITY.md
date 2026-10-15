# KnowledgeHub AI - Security Architecture & Threat Model

Security is built into KnowledgeHub AI as a fundamental design requirement across authentication, authorization, file ingestion, LLM prompting, and database access.

---

## 1. Authentication & Session Security

- **Stateless JWT**: Signed using HMAC-SHA256 with minimum 256-bit secret key.
- **BCrypt Password Hashing**: Passwords hashed with BCrypt (cost factor 10) before storage; plaintext passwords are never logged or stored.
- **Filter-Chain Enforcement**: `JwtAuthenticationFilter` intercepts requests before Spring Security's `UsernamePasswordAuthenticationFilter`. Unauthenticated requests to protected endpoints return sanitized 401 JSON responses without leaking internal stack traces.

---

## 2. Resource Ownership & Isolation

- **Zero Cross-Tenant Leakage**: Every document, chunk, chat session, and message is tagged with the owning `userId`.
- Repository methods enforce ownership checks:
  ```java
  documentRepository.findByIdAndUserId(documentId, userId);
  sessionRepository.findByIdAndUserId(sessionId, userId);
  ```
- Retrieval queries in `VectorSearchService` filter chunks by the authenticated user's ID: users can never search or retrieve other users' documents.

---

## 3. Ingestion & File Upload Security

- **Path Traversal Defense**: Files are never stored under their user-supplied original filename on disk. Uploaded files are assigned cryptographically random UUID filenames (`UUID.randomUUID().toString() + "." + extension`).
- **File Type Whitelist**: Only `.pdf`, `.docx`, `.txt`, `.md`, and `.csv` are allowed. Executable formats (`.exe`, `.sh`, `.bat`, `.jar`, `.js`, `.py`) are rejected at the controller gate.
- **Size Limitation**: 25MB hard request size limit enforced by Spring multipart config and controller validation.

---

## 4. Indirect Prompt Injection Defense

Because enterprise knowledge bases ingest external and untrusted documents, KnowledgeHub AI applies prompt shielding:
- Document excerpts are placed strictly within isolated `CONTEXT:` delimiters.
- The system instructions explicitly order the LLM to ignore any instructions inside context files that attempt to override system rules, policy constraints, or identity.

---

## 5. Centralized Error Sanitization

The `GlobalExceptionHandler` intercepts all runtime exceptions and converts them into standardized `ErrorResponse` objects:
- No database query errors, SQL exceptions, or Java stack traces are exposed to clients.
- Log correlation preserves internal errors in backend application logs for administrator diagnostics.

---

## 6. Audit Logging

Sensitive actions are captured asynchronously in the `audit_events` table:
- `LOGIN`, `REGISTER`
- `UPLOAD`, `RETRY`, `DELETE`
- `CHAT` (query submission)
- `EXPORT` (bundle download)
Capturing timestamp, action, target resource ID, and client IP address.
