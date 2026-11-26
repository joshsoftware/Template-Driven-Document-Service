package com.joshsoftware.templateDrivenDocumentService.exception;

import java.util.UUID;

// A job's queued payload cannot be saved or found. Message carries the job id only, never payload data.
public class JobPayloadException extends RuntimeException {

    public JobPayloadException(String reason, UUID jobId) {
        super("%s: jobId=%s".formatted(reason, jobId));
    }
}
