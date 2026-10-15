# KnowledgeHub AI - Testing Guide

KnowledgeHub AI includes real, automated unit and integration tests verifying business logic, security rules, document parsing, OKF compliance, and RAG grounding.

---

## 1. Backend Automated Tests

To run the backend test suite:
```bash
cd backend
mvn test
```

### Test Suites Included:
1. **`OkfValidatorTest`**:
   - `testValidOkf`: Verifies valid OKF v0.2 frontmatter parsing.
   - `testMissingTypeField`: Verifies that documents missing the required `type` field fail validation.
   - `testMissingTitleField`: Verifies that documents missing the required `title` field fail validation.
   - `testMalformedYaml`: Verifies safe failure on corrupt YAML.
   - `testUnicodeAndSpecialChars`: Verifies Unicode (Chinese, Japanese, Hindi, accented characters) and quotes.
2. **`DocumentParserTest`**:
   - Tests Markdown parser heading extraction and document title detection.
   - Tests TXT parser paragraph boundary detection.
   - Tests CSV parser tabular serialization.
   - Tests `DocumentParserFactory` extension resolution.
3. **`TextChunkerTest`**:
   - Tests sliding window chunking with configurable overlap.
   - Tests sentence boundary preservation.
   - Tests single-chunk preservation for short documents.
4. **`RagServiceTest`**:
   - `testInsufficientEvidenceRefusal`: Verifies that queries with no matching evidence return a clear refusal and 0 citations.
   - `testGroundedResponseWithCitations`: Verifies that grounded queries attach structured citations with document title, page number, section name, and similarity score.
5. **`AuthServiceTest`**:
   - Tests user registration and duplicate checks.
   - Tests BCrypt password verification and JWT token generation.
   - Tests rejection on invalid credentials.

### Actual Test Execution Output:
```
[INFO] Running com.knowledgehub.AuthServiceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.knowledgehub.DocumentParserTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.knowledgehub.OkfValidatorTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.knowledgehub.RagServiceTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.knowledgehub.TextChunkerTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 2. Frontend Build Verification

To verify TypeScript contracts, React components, and build bundle:
```bash
cd frontend
npm run build
```

Build verification compiles all 1,795 modules with zero TypeScript errors into the production bundle in `dist/`.
