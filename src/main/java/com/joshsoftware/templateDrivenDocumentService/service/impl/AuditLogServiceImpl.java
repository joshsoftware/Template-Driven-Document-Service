package com.joshsoftware.templateDrivenDocumentService.service.impl;

import com.joshsoftware.templateDrivenDocumentService.entity.AuditLog;
import com.joshsoftware.templateDrivenDocumentService.entity.DocumentJob;
import com.joshsoftware.templateDrivenDocumentService.enums.AuditAction;
import com.joshsoftware.templateDrivenDocumentService.enums.AuditOutcome;
import com.joshsoftware.templateDrivenDocumentService.repository.AuditLogRepository;
import com.joshsoftware.templateDrivenDocumentService.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    // MANDATORY: a row written outside the caller's transaction would not commit with its change, so fail loudly
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(DocumentJob job, AuditAction action, AuditOutcome outcome, String channel,
                       LocalDateTime createdAt) {
        AuditLog entry = new AuditLog();
        entry.setDocumentJob(job);
        entry.setAction(action);
        entry.setOutcome(outcome);
        // No caller authentication in Stage 1, so there is no client id yet
        entry.setChannel(channel);
        entry.setCreatedAt(createdAt);
        auditLogRepository.save(entry);
        log.debug("[AuditLogServiceImpl#record] action={} outcome={} channel={}", action, outcome, channel);
    }
}
