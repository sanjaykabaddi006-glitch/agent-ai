package com.knowledgehub.domain.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, String> {
    List<ProcessingJob> findByDocumentIdOrderByStartedAtDesc(String documentId);
    void deleteByDocumentId(String documentId);
}
