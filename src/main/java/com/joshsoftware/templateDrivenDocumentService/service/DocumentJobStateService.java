package com.joshsoftware.templateDrivenDocumentService.service;

import com.joshsoftware.templateDrivenDocumentService.enums.ErrorCode;

import java.util.UUID;

// Final state changes of a document job. Each one commits together with its GENERATE audit row.
public interface DocumentJobStateService {

    // Job READY with the stored PDF's key; audit GENERATE SUCCESS
    void markReady(UUID jobId, String channel, String storagePath);

    // Job FAILED with the error code; audit GENERATE FAILURE
    void markFailed(UUID jobId, String channel, ErrorCode errorCode);
}
