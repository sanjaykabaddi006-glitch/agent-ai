package com.knowledgehub.domain.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {
    List<AuditEvent> findByUserIdOrderByCreatedAtDesc(String userId);
    Page<AuditEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
