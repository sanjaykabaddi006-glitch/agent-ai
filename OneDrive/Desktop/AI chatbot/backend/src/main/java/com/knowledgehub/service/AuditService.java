package com.knowledgehub.service;

import com.knowledgehub.domain.audit.AuditEvent;
import com.knowledgehub.domain.audit.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Async
    public void log(String userId, String action, String resourceType, String resourceId, String details, String ipAddress) {
        try {
            AuditEvent event = new AuditEvent(userId, action, resourceType, resourceId, details, ipAddress);
            auditEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Failed to record audit event: {}", e.getMessage());
        }
    }
}
