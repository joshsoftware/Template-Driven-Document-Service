package com.joshsoftware.templateDrivenDocumentService.service;

import com.joshsoftware.templateDrivenDocumentService.dto.DocumentDownload;

import java.io.IOException;
import java.io.OutputStream;

// Download of a generated PDF: only the job id is checked (no caller auth, no expiry window yet)
public interface DocumentDownloadService {

    // Looks up the job, checks it can be downloaded and opens its PDF; the caller must stream or close it.
    // JobNotFoundException (404), DocumentUnavailableException (409/410), DocumentMissingException (410),
    // StorageException (503).
    DocumentDownload open(String jobId);

    // Copies the PDF to the response and audits the attempt: DOWNLOAD SUCCESS, or FAILURE when the copy breaks
    void stream(DocumentDownload download, OutputStream out) throws IOException;
}
