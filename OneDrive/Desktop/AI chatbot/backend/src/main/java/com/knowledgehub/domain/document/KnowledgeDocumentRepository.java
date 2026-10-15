package com.knowledgehub.domain.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, String> {

    Page<KnowledgeDocument> findByUserId(String userId, Pageable pageable);

    @Query("SELECT d FROM KnowledgeDocument d WHERE d.userId = :userId AND " +
           "(LOWER(d.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<KnowledgeDocument> findByUserIdAndSearch(@Param("userId") String userId, @Param("search") String search, Pageable pageable);

    Optional<KnowledgeDocument> findByIdAndUserId(String id, String userId);

    List<KnowledgeDocument> findByUserIdOrderByCreatedAtDesc(String userId);

    long countByUserId(String userId);

    long countByUserIdAndStatus(String userId, DocumentStatus status);

    List<KnowledgeDocument> findTop5ByUserIdOrderByCreatedAtDesc(String userId);
}
