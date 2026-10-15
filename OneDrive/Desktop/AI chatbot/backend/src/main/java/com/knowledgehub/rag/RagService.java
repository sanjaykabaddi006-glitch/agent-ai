package com.knowledgehub.rag;

import com.knowledgehub.domain.document.KnowledgeDocument;
import com.knowledgehub.domain.document.KnowledgeDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final OllamaEmbeddingService embeddingService;
    private final OllamaChatService chatService;
    private final VectorSearchService vectorSearchService;
    private final KnowledgeDocumentRepository documentRepository;

    public RagService(OllamaEmbeddingService embeddingService,
                      OllamaChatService chatService,
                      VectorSearchService vectorSearchService) {
        this(embeddingService, chatService, vectorSearchService, null);
    }

    @Autowired
    public RagService(OllamaEmbeddingService embeddingService,
                      OllamaChatService chatService,
                      VectorSearchService vectorSearchService,
                      KnowledgeDocumentRepository documentRepository) {
        this.embeddingService = embeddingService;
        this.chatService = chatService;
        this.vectorSearchService = vectorSearchService;
        this.documentRepository = documentRepository;
    }

    public RagResponse query(String userQuestion, String userId, List<OllamaChatService.ChatMessageDto> history,
                             Integer topK, Double similarityThreshold, Boolean enableHybrid) {
        return query(userQuestion, userId, null, history, topK, similarityThreshold, enableHybrid);
    }

    public RagResponse query(String userQuestion, String userId, String documentId,
                             List<OllamaChatService.ChatMessageDto> history,
                             Integer topK, Double similarityThreshold, Boolean enableHybrid) {
        long startTime = System.currentTimeMillis();

        if (userQuestion == null || userQuestion.trim().isEmpty()) {
            return new RagResponse("Please provide a question to search your documents.", List.of(), false, 0);
        }

        String trimmedQuestion = userQuestion.trim();

        // 1. Check for conversational greetings / intro questions
        if (isGreetingOrConversational(trimmedQuestion)) {
            String greetingResponse = buildConversationalGreeting(userId);
            long latency = System.currentTimeMillis() - startTime;
            return new RagResponse(greetingResponse, List.of(), true, latency);
        }

        // 2. Generate query embedding
        List<Double> queryVector = embeddingService.generateEmbedding(trimmedQuestion);

        // 3. Retrieve authorized document chunks scoped to attached document
        List<RetrievalResult> retrievedChunks = vectorSearchService.search(
                queryVector, trimmedQuestion, userId, documentId, topK, similarityThreshold, enableHybrid
        );

        // If no relevant documents found even after search
        if (retrievedChunks.isEmpty()) {
            if (documentRepository != null) {
                List<KnowledgeDocument> userDocs = documentRepository.findByUserIdOrderByCreatedAtDesc(userId);
                if (userDocs.isEmpty()) {
                    userDocs = documentRepository.findAll();
                }
                if (userDocs.isEmpty()) {
                    long latency = System.currentTimeMillis() - startTime;
                    return new RagResponse("You haven't uploaded any business documents yet. Please upload files in the Documents tab or attach them here to ask questions.", List.of(), false, latency);
                }
            }
            long latency = System.currentTimeMillis() - startTime;
            return new RagResponse("The uploaded document does not contain this information. I can only answer questions based on the content of your uploaded document.", List.of(), false, latency);
        }

        // 4. NLP Intent Detection: Full document details / summary vs specific item
        boolean isFullDoc = VectorSearchService.isFullDocumentRequest(trimmedQuestion);
        int maxChunks = isFullDoc ? Math.min(50, retrievedChunks.size()) : Math.min(topK != null ? Math.min(topK, 5) : 4, retrievedChunks.size());

        StringBuilder contextBuilder = new StringBuilder();
        List<Citation> citations = new ArrayList<>();

        for (int i = 0; i < maxChunks; i++) {
            RetrievalResult chunk = retrievedChunks.get(i);
            citations.add(chunk.toCitation());

            contextBuilder.append("[").append(i + 1).append("] Document: \"")
                    .append(chunk.getDocumentTitle()).append("\"");
            if (chunk.getPageNumber() != null) {
                contextBuilder.append(" (Page ").append(chunk.getPageNumber()).append(")");
            }
            if (chunk.getSectionName() != null && !chunk.getSectionName().isBlank()) {
                contextBuilder.append(" - Section: \"").append(chunk.getSectionName()).append("\"");
            }
            contextBuilder.append("\n");
            contextBuilder.append(chunk.getContent()).append("\n\n");
        }

        String systemPrompt = isFullDoc ? buildFullDocumentPrompt(contextBuilder.toString()) : buildSpecificPrompt(contextBuilder.toString());
        int tokenBudget = isFullDoc ? 2500 : 350;
        log.info("NLP Intent: {} (assembled {} chunks, chars: {}). Calling Ollama with token budget: {}...",
                isFullDoc ? "FULL_DOCUMENT_DETAILS" : "SPECIFIC_ITEM", maxChunks, contextBuilder.length(), tokenBudget);

        // 5. Generate answer from Ollama
        String answer = chatService.generateAnswer(systemPrompt, trimmedQuestion, history, tokenBudget);
        log.info("Ollama response received: '{}'", answer);
        if (answer == null) {
            answer = "The uploaded document does not contain this information.";
        }

        // 6. Measure latency and return response
        long latency = System.currentTimeMillis() - startTime;
        boolean isInsufficient = answer.toLowerCase().contains("do not provide enough information")
                || answer.toLowerCase().contains("insufficient evidence")
                || answer.toLowerCase().contains("no direct answer could be generated")
                || answer.toLowerCase().contains("does not contain this information")
                || answer.toLowerCase().contains("not found in the uploaded document")
                || answer.toLowerCase().contains("not present in the uploaded document")
                || answer.toLowerCase().contains("only answer questions based on the content");

        return new RagResponse(answer.trim(), isInsufficient ? List.of() : citations, !isInsufficient, latency);
    }

    private boolean isGreetingOrConversational(String text) {
        String clean = text.toLowerCase().replaceAll("[^a-z0-9 ]", " ").trim();
        // Only return true for standalone, short greetings with no substantive question or document query
        return clean.matches("^(hi|hello|hey|good morning|good afternoon|good evening|who are you|what can you do|help|howdy|greetings)$");
    }

    private String buildConversationalGreeting(String userId) {
        if (documentRepository == null) {
            return "Hello! I am **KnowledgeHub AI**, your enterprise knowledge assistant. How can I help you today?";
        }

        List<KnowledgeDocument> userDocs = documentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (userDocs.isEmpty()) {
            return "Hello! I am **KnowledgeHub AI**, your enterprise knowledge assistant.\n\n" +
                   "You haven't uploaded any business documents yet. Please visit the **Documents** tab to upload PDF, DOCX, PPT, PPTX, TXT, or Markdown files, and I'll be ready to answer questions about them!";
        }

        String docNames = userDocs.stream()
                .limit(3)
                .map(d -> "`" + d.getTitle() + "`")
                .collect(Collectors.joining(", "));

        return "Hello! I am **KnowledgeHub AI**, your enterprise knowledge assistant.\n\n" +
               "I currently have access to your uploaded knowledge base (including " + docNames + "). " +
               "Feel free to ask me to summarize your documents, extract key details, or answer specific questions!";
    }

    private String buildFullDocumentPrompt(String contextText) {
        return """
               You are KnowledgeHub AI, a strictly grounded, thorough enterprise document assistant.
               The user is asking to explain or receive the FULL, COMPLETE content and all details from their uploaded document/file.
               
               ABSOLUTE STRICT GROUNDING RULES:
               1. ANSWER ONLY AND EXCLUSIVELY FROM THE CONTEXT BELOW. NEVER PROVIDE ANY INFORMATION OUT OF THE DOCUMENT.
               2. ZERO EXTERNAL KNOWLEDGE: Do NOT use any pre-trained world knowledge, outside assumptions, or unmentioned facts. Every single detail must come directly from the uploaded file text.
               3. Provide the FULL, THOROUGH, and COMPLETE content from the CONTEXT below.
               4. DO NOT cut off. DO NOT stop in the middle of a sentence or bullet point. Complete all bullet points, sections, and thoughts thoroughly.
               5. DO NOT give a short summary or half an answer. Cover EVERY topic, requirement, challenge, specification, and detail present in the text.
               6. Organize your answer with clear markdown headings (##, ###) and detailed bullet points for each section.
               7. Be 100% faithful and strictly factual to the uploaded document text.
               
               CONTEXT:
               """ + contextText;
    }

    private String buildSpecificPrompt(String contextText) {
        return """
               You are KnowledgeHub AI, an uncompromising, strictly grounded document intelligence system.
               Your ONLY source of truth is the provided DOCUMENT CONTEXT below.
               
               ABSOLUTE STRICT GROUNDING RULES:
               1. ANSWER ONLY AND EXCLUSIVELY BASED ON THE UPLOADED DOCUMENT CONTEXT BELOW.
               2. ZERO EXTERNAL KNOWLEDGE: You are strictly forbidden from answering using general knowledge, outside world facts, pre-trained information, or assumptions.
               3. NEVER GIVE ANYTHING OUT OF THE DOCUMENT: If the user asks about something not explicitly mentioned or found in the CONTEXT below, or asks an unrelated general question, you MUST NOT answer from outside knowledge. Instead, reply immediately with:
               "The uploaded document does not contain this information. I can only answer questions directly based on the content of your uploaded document."
               4. If the user asks about a specific problem statement, numbered section (e.g., problem 6), table, or bullet point, answer strictly and solely from that exact item as written in the CONTEXT. Do NOT mix other sections.
               5. Use clear, organized bullet points and bold highlights for maximum clarity.
               
               CONTEXT:
               """ + contextText;
    }
}
