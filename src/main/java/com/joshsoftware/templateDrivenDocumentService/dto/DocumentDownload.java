package com.joshsoftware.templateDrivenDocumentService.dto;

import java.util.UUID;

// A download ready to stream: the open PDF plus what the response and the audit row need
public record DocumentDownload(UUID jobId, String fileName, String channel, StoredDocument document) {
}
