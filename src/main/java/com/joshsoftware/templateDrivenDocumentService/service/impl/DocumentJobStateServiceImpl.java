package com.joshsoftware.templateDrivenDocumentService.service.impl;

import com.joshsoftware.templateDrivenDocumentService.annotation.Audit;
import com.joshsoftware.templateDrivenDocumentService.entity.DocumentJob;
import com.joshsoftware.templateDrivenDocumentService.enums.AuditAction;
import com.joshsoftware.templateDrivenDocumentService.enums.ErrorCode;
import com.joshsoftware.templateDrivenDocumentService.enums.JobStatus;
import com.joshsoftware.templateDrivenDocumentService.repository.DocumentJobRepository;
import com.joshsoftware.templateDrivenDocumentService.service.DocumentJobStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

// Its own bean so the @Transactional and @Audit proxies apply: the generation service calls it from outside
@Service
@RequiredArgsConstructor
public class DocumentJobStateServiceImpl implements DocumentJobStateService {

    private final DocumentJobRepository documentJobRepository;
    private final Clock clock;

    @Override
    @Transactional
    @Audit(action = AuditAction.GENERATE, outcome = Audit.Outcome.SUCCESS, jobId = "#jobId", channel = "#channel")
    public void markReady(UUID jobId, String channel, String storagePath) {
        DocumentJob job = documentJobRepository.getReferenceById(jobId);
        job.setStatus(JobStatus.READY);
        job.setStoragePath(storagePath);
        job.setCompletedAt(now());
    }

    @Override
    @Transactional
    @Audit(action = AuditAction.GENERATE, outcome = Audit.Outcome.FAILURE, jobId = "#jobId", channel = "#channel")
    public void markFailed(UUID jobId, String channel, ErrorCode errorCode) {
        DocumentJob job = documentJobRepository.getReferenceById(jobId);
        job.setStatus(JobStatus.FAILED);
        job.setErrorCode(errorCode.name());
        job.setCompletedAt(now());
    }

    // Timestamp columns are TIMESTAMPTZ written as UTC (hibernate.jdbc.time_zone)
    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
