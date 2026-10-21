package com.financemanager.service;

import com.financemanager.entity.AuditEvent;
import com.financemanager.entity.User;
import com.financemanager.repository.AuditEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    @Autowired
    private AuditEventRepository auditEventRepository;

    public void log(User actor, String action, String targetType, String targetId, String metadata) {
        try {
            AuditEvent event = new AuditEvent(actor, action, targetType, targetId, metadata);
            auditEventRepository.save(event);
        } catch (Exception ignored) {
            // Non-blocking audit logger
        }
    }
}
