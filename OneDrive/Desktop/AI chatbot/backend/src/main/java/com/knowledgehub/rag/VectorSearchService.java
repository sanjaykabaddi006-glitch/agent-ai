package com.knowledgehub.rag;

import com.knowledgehub.config.KnowledgeHubProperties;
import com.knowledgehub.domain.document.DocumentChunk;
import com.knowledgehub.domain.document.DocumentChunkRepository;
import com.knowledgehub.domain.document.KnowledgeDocument;
import com.knowledgehub.domain.document.KnowledgeDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);

    private final DocumentChunkRepository chunkRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeHubProperties properties;

    public VectorSearchService(DocumentChunkRepository chunkRepository,
                               KnowledgeDocumentRepository documentRepository,
                               KnowledgeHubProperties properties) {
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
        this.properties = properties;
    }

    public List<RetrievalResult> search(List<Double> queryVector, String queryText, String userId,
                                        Integer topK, Double similarityThreshold, Boolean enableHybrid) {
        return search(queryVector, queryText, userId, null, topK, similarityThreshold, enableHybrid);
    }

    public List<RetrievalResult> search(List<Double> queryVector, String queryText, String userId,
                                        String targetDocumentId, Integer topK, Double similarityThreshold, Boolean enableHybrid) {
        if (queryVector == null) {
            queryVector = Collections.emptyList();
        }

        int k = topK != null ? topK : properties.getRag().getTopK();
        double threshold = similarityThreshold != null ? similarityThreshold : properties.getRag().getSimilarityThreshold();
        boolean hybrid = enableHybrid != null ? enableHybrid : properties.getRag().isHybridSearch();

        List<DocumentChunk> userChunks = chunkRepository.findChunksWithEmbeddingsByUserId(userId);
        if (userChunks.isEmpty()) {
            userChunks = chunkRepository.findByUserId(userId);
        }
        if (userChunks.isEmpty()) {
            userChunks = chunkRepository.findAll();
        }
        if (userChunks.isEmpty()) {
            return Collections.emptyList();
        }

        // Cache document titles
        Map<String, String> docTitles = new HashMap<>();
        for (DocumentChunk chunk : userChunks) {
            docTitles.computeIfAbsent(chunk.getDocumentId(), id ->
                    documentRepository.findById(id).map(KnowledgeDocument::getTitle).orElse("Document " + id)
            );
        }

        String queryLower = normalizeQueryText(queryText);
        Set<String> queryKeywords = extractKeywords(queryLower);

        // 1. Resolve which document should be scoped (attached document or most relevant/recent)
        String activeDocId = resolveTargetDocumentId(queryText, userId, targetDocumentId, userChunks, docTitles);

        // If a specific document is identified/attached, scope chunks strictly to that document
        if (activeDocId != null) {
            List<DocumentChunk> scopedChunks = userChunks.stream()
                    .filter(c -> activeDocId.equals(c.getDocumentId()))
                    .collect(Collectors.toList());
            if (!scopedChunks.isEmpty()) {
                userChunks = scopedChunks;
                log.info("Search strictly scoped to document '{}' ({}) with {} chunks",
                        docTitles.getOrDefault(activeDocId, "Target Doc"), activeDocId, userChunks.size());
            }
        }

        // 2. NLP Intent Check: If user asks for full document details, entire file, complete overview, or summary
        if (isFullDocumentRequest(queryText)) {
            List<RetrievalResult> fullDocResults = retrieveFullDocumentChunks(queryText, userId, activeDocId, userChunks, docTitles, 60);
            if (!fullDocResults.isEmpty()) {
                return fullDocResults;
            }
        }

        List<RetrievalResult> candidateResults = new ArrayList<>();
        log.info("Searching {} user chunks for query: '{}'. Extracted keywords: {}", userChunks.size(), queryText, queryKeywords);

        for (DocumentChunk chunk : userChunks) {
            List<Double> chunkVector = chunk.getVectorList();
            double cosineSim = 0.0;
            if (queryVector != null && !queryVector.isEmpty() && chunkVector != null && !chunkVector.isEmpty() && chunkVector.size() == queryVector.size()) {
                cosineSim = computeCosineSimilarity(queryVector, chunkVector);
            }

            String docTitle = docTitles.getOrDefault(chunk.getDocumentId(), "Unknown");
            double finalScore = cosineSim;

            double keywordOverlap = computeKeywordOverlap(chunk.getContent(), queryKeywords);
            boolean titleMatches = matchesTitle(docTitle, queryKeywords);
            boolean sectionMatches = matchesSection(chunk.getSectionName(), queryKeywords);

            // Base score from vector similarity and keyword overlap
            if (hybrid && !queryKeywords.isEmpty()) {
                finalScore = Math.max(cosineSim, (0.40 * cosineSim) + (0.60 * keywordOverlap));
            } else if (keywordOverlap > 0) {
                finalScore = Math.max(finalScore, keywordOverlap);
            }

            // Section match boost: if the chunk's section specifically matches the asked topic (e.g. SKILLS, PROJECT, EDUCATION)
            if (sectionMatches) {
                finalScore += 0.55;
            }

            // Topic keywords: words in the query that specifically ask about content (not just document title or file words)
            Set<String> titleWords = extractKeywords(docTitle);
            Set<String> topicKeywords = queryKeywords.stream()
                    .filter(kw -> !titleWords.contains(kw) && !kw.equalsIgnoreCase("document") && !kw.equalsIgnoreCase("file")
                            && !kw.equalsIgnoreCase("pdf") && !kw.equalsIgnoreCase("docx") && !kw.equalsIgnoreCase("pptx") && !kw.equalsIgnoreCase("ppt"))
                    .collect(Collectors.toSet());

            double topicOverlap = computeKeywordOverlap(chunk.getContent(), topicKeywords);
            if (!topicKeywords.isEmpty() && topicOverlap > 0) {
                finalScore += (topicOverlap * 0.50);
            }

            // Keyword density boost: chunks directly containing query terms get strong boost
            if (keywordOverlap > 0.15) {
                finalScore += (keywordOverlap * 0.45);
            }

            // Exact phrase & n-gram matching (e.g., "problem statement 1", "route optimization")
            String queryClean = queryLower.replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
            String chunkClean = chunk.getContent().toLowerCase().replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ");
            if (!queryClean.isBlank()) {
                String[] queryTokens = queryClean.split(" ");
                for (int i = 0; i < queryTokens.length - 1; i++) {
                    if (!STOP_WORDS.contains(queryTokens[i]) || !STOP_WORDS.contains(queryTokens[i + 1])) {
                        String bigram = queryTokens[i] + " " + queryTokens[i + 1];
                        if (chunkClean.contains(bigram)) {
                            finalScore += 0.40;
                        }
                    }
                }
                for (int i = 0; i < queryTokens.length - 2; i++) {
                    String trigram = queryTokens[i] + " " + queryTokens[i + 1] + " " + queryTokens[i + 2];
                    if (chunkClean.contains(trigram)) {
                        finalScore += 0.60;
                    }
                }
            }

            // Specific number / section targeting (e.g. "6" or "problem 6" or "statement 6")
            for (String kw : queryKeywords) {
                if (kw.matches("\\d+")) {
                    String pattern1 = kw + ".";
                    String pattern2 = "problem " + kw;
                    String pattern3 = "statement " + kw;
                    String pattern4 = kw + " ";
                    if (chunkClean.contains(pattern1) || chunkClean.contains(pattern2) || chunkClean.contains(pattern3) || chunkClean.startsWith(pattern4)) {
                        finalScore += 1.50; // Decisive boost for exact numbered problem statement!
                    }
                }
            }

            // Title match boost (modest, so intra-document chunks compete on actual content relevance)
            if (titleMatches) {
                finalScore += 0.10;
            }

            // Direct title or filename reference boost
            String cleanTitle = docTitle.toLowerCase().replaceAll("\\.[^.]+$", "").trim();
            if (!cleanTitle.isBlank() && queryLower.contains(cleanTitle)) {
                finalScore += 0.15;
            }

            // If broad overview/summary query, ensure baseline score for top chunks
            if (isOverviewQuery(queryLower)) {
                finalScore = Math.max(finalScore, 0.30 + (cosineSim > 0 ? cosineSim * 0.5 : 0.05));
            }

            candidateResults.add(new RetrievalResult(
                    chunk.getId(),
                    chunk.getDocumentId(),
                    docTitle,
                    chunk.getContent(),
                    chunk.getPageNumber(),
                    chunk.getSectionName(),
                    chunk.getChunkIndex(),
                    finalScore
            ));
        }

        // Sort descending by score
        candidateResults.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        log.info("Top candidates found: {}", candidateResults.stream()
                .limit(5)
                .map(c -> c.getDocumentTitle() + " [sec=" + c.getSectionName() + ", score=" + String.format("%.3f", c.getScore()) + "]")
                .toList());

        // Primary filter: candidates meeting threshold
        List<RetrievalResult> filtered = candidateResults.stream()
                .filter(res -> res.getScore() >= threshold)
                .collect(Collectors.toList());

        // Adaptive fallback: only if the top candidate has reasonable semantic or keyword connection
        // (Prevents unrelated/out-of-document queries from pulling random low-similarity chunks)
        if (filtered.isEmpty() && !candidateResults.isEmpty()) {
            double topScore = candidateResults.get(0).getScore();
            if (topScore >= 0.20 || isOverviewQuery(queryLower)) {
                filtered = candidateResults.stream()
                        .filter(res -> res.getScore() >= Math.max(0.18, topScore - 0.15))
                        .collect(Collectors.toList());
            }
        }

        // Deduplicate chunks that are contiguous or practically identical
        List<RetrievalResult> deduplicated = new ArrayList<>();
        Set<String> seenSnippets = new HashSet<>();

        for (RetrievalResult res : filtered) {
            String normSnippet = res.getContent().replaceAll("\\s+", " ").toLowerCase();
            String prefix = normSnippet.substring(0, Math.min(60, normSnippet.length()));
            if (!seenSnippets.contains(prefix)) {
                seenSnippets.add(prefix);
                deduplicated.add(res);
                if (deduplicated.size() >= k) {
                    break;
                }
            }
        }

        return deduplicated;
    }

    public static boolean isFullDocumentRequest(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase().trim();

        // If the user specifies a specific problem number, section, question number, or field, it is a SPECIFIC question, NOT a full document dump!
        if (lower.matches(".*\\b(problem\\s*\\d+|statement\\s*\\d+|section\\s*\\d+|part\\s*\\d+|question\\s*\\d+|item\\s*\\d+|step\\s*\\d+|case\\s*\\d+|test\\s*case\\s*\\d+|page\\s*\\d+)\\b.*")
                || lower.matches(".*\\b(qualification|education|experience|skills?|salary|email|phone|contact|address|deadline|criteria|eligibility)\\b.*")) {
            return false;
        }

        // 1. Direct phrases with full / all / complete / entire / whole / fully / explain
        if (lower.contains("full document") || lower.contains("full documents") || lower.contains("full details")
                || lower.contains("all details") || lower.contains("complete details") || lower.contains("entire document")
                || lower.contains("whole document") || lower.contains("full info") || lower.contains("full information")
                || lower.contains("all information") || lower.contains("complete information") || lower.contains("everything in")
                || lower.contains("all data") || lower.contains("full content") || lower.contains("full summary")
                || lower.contains("entire file") || lower.contains("whole file") || lower.contains("all content")
                || lower.contains("complete profile") || lower.contains("full profile") || lower.contains("every detail")
                || lower.contains("all the details") || lower.contains("full document details") || lower.contains("full documents details")
                || lower.contains("full give") || lower.contains("give full") || lower.contains("give fully")
                || lower.contains("fully give") || lower.contains("ask fully") || lower.contains("ask full")
                || lower.contains("full text") || lower.contains("all text") || lower.contains("whole text")
                || lower.contains("entire text") || lower.contains("give complete") || lower.contains("show full")
                || lower.contains("show all") || lower.contains("display full") || lower.contains("read full")
                || lower.contains("give everything") || lower.contains("show everything") || lower.contains("all file")
                || lower.contains("complete file") || lower.contains("not give shortly") || lower.contains("not shortly")
                || lower.contains("dont give shortly") || lower.contains("fine tune")
                || lower.contains("full pdf") || lower.contains("explain full") || lower.contains("explain pdf")
                || lower.contains("explain full pdf") || lower.contains("explain this pdf") || lower.contains("explain the pdf")
                || lower.contains("explain all") || lower.contains("explain file") || lower.contains("explain document")
                || lower.contains("explain the file") || lower.contains("tell me full") || lower.contains("tell me everything")
                || lower.contains("read pdf") || lower.contains("read full pdf") || lower.contains("what does this pdf")
                || lower.contains("what is this pdf") || lower.contains("describe pdf") || lower.contains("describe the pdf")
                || lower.contains("describe full") || lower.contains("full co tent") || lower.contains("full cotent")
                || lower.contains("fully half") || lower.contains("half only") || lower.contains("not half")) {
            return true;
        }

        // 2. Summary, overview, or complete breakdown keywords
        if (lower.contains("summarize") || lower.contains("summary") || lower.contains("overview")
                || lower.contains("key takeaways") || lower.contains("synopsis") || lower.contains("briefing")
                || lower.contains("high level overview") || lower.contains("executive summary")
                || lower.contains("complete breakdown") || lower.contains("comprehensive review")) {
            return true;
        }

        // 3. Questions asking what the file is about or asking to explain/describe the file
        if (lower.contains("what is in this") || lower.contains("what is in the") || lower.contains("what's in this")
                || lower.contains("what does this file") || lower.contains("what does this document")
                || lower.contains("what does this pdf") || lower.contains("what's in this pdf")
                || (lower.contains("explain") && (lower.contains("pdf") || lower.contains("file") || lower.contains("doc")))
                || (lower.contains("describe") && (lower.contains("pdf") || lower.contains("file") || lower.contains("doc")))
                || (lower.contains("tell me about") && (lower.contains("pdf") || lower.contains("file") || lower.contains("doc")))
                || lower.contains("about this document") || lower.contains("about this file") || lower.contains("about this pdf")) {
            return true;
        }

        // 4. NLP regex matching:
        // Any request combining full/fully/complete/entire/whole/all/everything with give/content/details/file/text/info/pdf
        if (lower.matches(".*\\b(full|fully|complete|entire|whole|all|comprehensive|everything)\\b.*\\b(give|content|details?|info(rmation)?|document|file|text|data|profile|resume|pdf|doc|docx|everything)\\b.*")) {
            return true;
        }

        // Inverse order: (pdf|file|document|doc|content) ... (full|fully|all|complete|entire|whole|everything)
        if (lower.matches(".*\\b(pdf|file|document|doc|content|text)\\b.*\\b(full|fully|all|complete|entire|whole|everything)\\b.*")) {
            return true;
        }

        // Action verbs: (explain|give|show|print|display|tell|read|fetch|get|provide|describe|summarize) ... (full|fully|all|everything|complete|entire|whole|pdf|file|document)
        if (lower.matches(".*\\b(explain|give|show|print|display|tell|read|fetch|get|provide|describe|summarize)\\b.*\\b(full|fully|all|everything|complete|entire|whole|pdf|file|document)\\b.*")) {
            return true;
        }

        // Requests explicitly rejecting short summaries ("not shortly", "don't summarize", etc.)
        if (lower.matches(".*\\b(not|never|don't|dont|half)\\b.*\\b(short|shortly|brief|briefly|cut|truncate|half)\\b.*")) {
            return true;
        }

        // 5. Query explicitly asking about this document/file/candidate without a specific field target
        if (lower.contains("tell me about this") || lower.contains("tell me about the candidate") || lower.contains("tell me about the applicant")
                || lower.contains("who is this candidate") || lower.contains("who is this person") || lower.contains("who is the candidate")
                || lower.contains("who is the applicant") || lower.contains("candidate profile") || lower.contains("applicant profile")
                || (lower.startsWith("tell me about") && (lower.contains("document") || lower.contains("file") || lower.contains("resume") || lower.contains("pdf")))) {
            boolean hasSpecificTarget = lower.contains("skill") || lower.contains("phone") || lower.contains("email")
                    || lower.contains("degree") || lower.contains("college") || lower.contains("address")
                    || lower.contains("project") || lower.contains("experience") || lower.contains("salary");
            if (!hasSpecificTarget) {
                return true;
            }
        }

        return false;
    }

    private String resolveTargetDocumentId(String queryText, String userId, String targetDocumentId,
                                          List<DocumentChunk> userChunks, Map<String, String> docTitles) {
        if (targetDocumentId != null && !targetDocumentId.isBlank()) {
            return targetDocumentId.trim();
        }

        List<KnowledgeDocument> userDocs = documentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (userDocs.isEmpty()) {
            userDocs = documentRepository.findAll();
        }
        if (userDocs.isEmpty()) {
            return null;
        }

        Set<String> queryKeywords = extractKeywords(queryText);

        // 1. Check for specific unique keyword match in document title (e.g. "gobi", "sanjay", "handbook")
        KnowledgeDocument bestMatchDoc = null;
        int highestSpecificMatches = 0;

        for (KnowledgeDocument doc : userDocs) {
            String titleLower = doc.getTitle().toLowerCase();
            int specificMatches = 0;
            for (String kw : queryKeywords) {
                // Ignore ultra-generic words like "resume", "document", "file", "details" when identifying a specific file
                if (!kw.equalsIgnoreCase("resume") && !kw.equalsIgnoreCase("document")
                        && !kw.equalsIgnoreCase("file") && !kw.equalsIgnoreCase("details")
                        && titleLower.contains(kw)) {
                    specificMatches++;
                }
            }
            if (specificMatches > highestSpecificMatches) {
                highestSpecificMatches = specificMatches;
                bestMatchDoc = doc;
            }
        }

        if (bestMatchDoc != null && highestSpecificMatches > 0) {
            return bestMatchDoc.getId();
        }

        // 2. If query mentions "resume", "presentation", "handbook", etc., pick the MOST RECENT document of that type
        for (KnowledgeDocument doc : userDocs) {
            String titleLower = doc.getTitle().toLowerCase();
            for (String kw : queryKeywords) {
                if (titleLower.contains(kw)) {
                    return doc.getId(); // First one found in createdAt DESC order!
                }
            }
        }

        // 3. Default to the user's most recently uploaded document
        return userDocs.get(0).getId();
    }

    private List<RetrievalResult> retrieveFullDocumentChunks(String queryText, String userId, String targetDocId,
                                                            List<DocumentChunk> userChunks, Map<String, String> docTitles, int maxChunks) {
        String resolvedTargetId = targetDocId;
        if (resolvedTargetId == null) {
            resolvedTargetId = resolveTargetDocumentId(queryText, userId, null, userChunks, docTitles);
        }
        if (resolvedTargetId == null) {
            return Collections.emptyList();
        }

        final String finalDocId = resolvedTargetId;
        String targetTitle = docTitles.getOrDefault(finalDocId, "Document");

        // Fetch all chunks for this target document ordered by chunkIndex
        List<DocumentChunk> docChunks = chunkRepository.findByDocumentIdOrderByChunkIndexAsc(finalDocId);
        if (docChunks.isEmpty()) {
            docChunks = userChunks.stream()
                    .filter(c -> finalDocId.equals(c.getDocumentId()))
                    .sorted(Comparator.comparingInt(DocumentChunk::getChunkIndex))
                    .collect(Collectors.toList());
        }

        log.info("Full document query detected ('{}'). Retrieved {} ordered chunks for document '{}' ({})",
                queryText, docChunks.size(), targetTitle, finalDocId);

        List<RetrievalResult> results = new ArrayList<>();
        int count = Math.min(maxChunks, docChunks.size());
        for (int i = 0; i < count; i++) {
            DocumentChunk c = docChunks.get(i);
            results.add(new RetrievalResult(
                    c.getId(),
                    c.getDocumentId(),
                    targetTitle,
                    c.getContent(),
                    c.getPageNumber(),
                    c.getSectionName(),
                    c.getChunkIndex(),
                    1.0
            ));
        }

        return results;
    }

    private boolean isOverviewQuery(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase();
        return isFullDocumentRequest(lower);
    }

    private boolean matchesTitle(String title, Set<String> keywords) {
        if (title == null || title.isBlank() || keywords.isEmpty()) return false;
        String titleLower = title.toLowerCase();
        for (String kw : keywords) {
            if (kw.length() > 2 && titleLower.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesSection(String section, Set<String> keywords) {
        if (section == null || section.isBlank() || keywords.isEmpty()) return false;
        String secLower = section.toLowerCase();
        for (String kw : keywords) {
            if (kw.length() > 2 && (secLower.contains(kw) || kw.contains(secLower))) {
                return true;
            }
        }
        return false;
    }

    private double computeCosineSimilarity(List<Double> v1, List<Double> v2) {
        double dot = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < v1.size(); i++) {
            double a = v1.get(i);
            double b = v2.get(i);
            dot += a * b;
            norm1 += a * a;
            norm2 += b * b;
        }

        if (norm1 <= 0.0 || norm2 <= 0.0) {
            return 0.0;
        }

        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    public static String normalizeQueryText(String text) {
        if (text == null) return "";
        String s = text.toLowerCase();
        s = s.replaceAll("\\b(1st|first)\\b", "1");
        s = s.replaceAll("\\b(2nd|second)\\b", "2");
        s = s.replaceAll("\\b(3rd|third)\\b", "3");
        s = s.replaceAll("\\b(4th|fourth)\\b", "4");
        s = s.replaceAll("\\b(5th|fifth)\\b", "5");
        s = s.replaceAll("\\b(6th|sixth|6\\s*th)\\b", "6");
        s = s.replaceAll("\\b(7th|seventh|7\\s*th)\\b", "7");
        s = s.replaceAll("\\b(8th|eighth|8\\s*th)\\b", "8");
        s = s.replaceAll("\\b(9th|ninth|9\\s*th)\\b", "9");
        s = s.replaceAll("\\b(10th|tenth|10\\s*th)\\b", "10");
        return s;
    }

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "about", "above", "after", "again", "all", "also", "am", "an", "and", "any", "are", "as", "at",
            "be", "because", "been", "before", "being", "below", "between", "both", "but", "by", "can", "could",
            "did", "do", "does", "doing", "down", "during", "each", "few", "for", "from", "further", "had", "has",
            "have", "having", "he", "her", "here", "hers", "herself", "him", "himself", "his", "how", "i", "if",
            "in", "into", "is", "it", "its", "itself", "just", "me", "more", "most", "my", "myself", "no", "nor",
            "not", "now", "of", "off", "on", "once", "only", "or", "other", "our", "ours", "ourselves", "out",
            "over", "own", "same", "she", "should", "so", "some", "such", "than", "that", "the", "their", "theirs",
            "them", "themselves", "then", "there", "these", "they", "this", "those", "through", "to", "too",
            "under", "until", "up", "very", "was", "we", "were", "what", "when", "where", "which", "while",
            "who", "whom", "why", "with", "would", "you", "your", "yours", "tell", "give", "please", "want",
            "need", "find", "get", "ask", "put", "show", "like"
    );

    private Set<String> extractKeywords(String query) {
        if (query == null || query.isBlank()) return Collections.emptySet();
        return Arrays.stream(query.toLowerCase().split("\\W+"))
                .filter(w -> !w.isBlank() && !STOP_WORDS.contains(w) && (w.length() >= 2 || w.matches("\\d+")))
                .collect(Collectors.toSet());
    }

    private double computeKeywordOverlap(String content, Set<String> keywords) {
        if (content == null || content.isBlank() || keywords.isEmpty()) return 0.0;
        String lower = content.toLowerCase();
        int matches = 0;
        for (String kw : keywords) {
            if (lower.contains(kw)) {
                matches++;
            }
        }
        return (double) matches / (double) keywords.size();
    }
}
