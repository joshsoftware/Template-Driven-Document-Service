package com.joshsoftware.templateDrivenDocumentService.exception;

import com.joshsoftware.templateDrivenDocumentService.enums.ErrorCode;
import com.joshsoftware.templateDrivenDocumentService.enums.JobStatus;

import java.util.UUID;

// The job exists but its PDF cannot be downloaded: not ready (409), failed (409) or expired (410)
public class DocumentUnavailableException extends AppException {

    public DocumentUnavailableException(ErrorCode errorCode, UUID jobId, JobStatus status) {
        super(errorCode, "Document unavailable: jobId=%s status=%s".formatted(jobId, status));
    }
}
