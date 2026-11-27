package com.joshsoftware.templateDrivenDocumentService.dto;

import com.joshsoftware.templateDrivenDocumentService.enums.JobStatus;

import java.util.UUID;

// The job fields a download needs, read in one query without loading the template's HTML
public record JobDownloadView(UUID jobId, JobStatus status, String storagePath, String channel, String templateId) {
}
