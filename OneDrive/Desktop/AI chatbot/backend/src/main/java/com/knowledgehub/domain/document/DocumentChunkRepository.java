package com.knowledgehub.domain.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, String> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(String documentId);

    List<DocumentChunk> findByUserId(String userId);

    @Query("SELECT c FROM DocumentChunk c WHERE c.userId = :userId AND c.embeddingVectorJson IS NOT NULL")
    List<DocumentChunk> findChunksWithEmbeddingsByUserId(@Param("userId") String userId);

    @Transactional
    @Modifying
    @Query("DELETE FROM DocumentChunk c WHERE c.documentId = :documentId")
    void deleteByDocumentId(@Param("documentId") String documentId);

    long countByDocumentId(String documentId);
}
