package com.joshsoftware.templateDrivenDocumentService.service;

import com.joshsoftware.templateDrivenDocumentService.entity.DocumentJob;
import com.joshsoftware.templateDrivenDocumentService.enums.AuditAction;
import com.joshsoftware.templateDrivenDocumentService.enums.AuditOutcome;

import java.time.LocalDateTime;

// Writes the append-only audit trail. Rows are inserted, never updated or deleted.
public interface AuditLogService {

    // Inserts one audit row for the job. Joins the caller's transaction, so the row commits
    // (or rolls back) together with the job status change it describes.
    // createdAt is passed in so the row carries the same UTC timestamp as the job update.
    void record(DocumentJob job, AuditAction action, AuditOutcome outcome, String channel, LocalDateTime createdAt);
}
