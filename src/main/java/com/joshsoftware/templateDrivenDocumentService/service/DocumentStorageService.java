package com.joshsoftware.templateDrivenDocumentService.service;

import com.joshsoftware.templateDrivenDocumentService.dto.StoredDocument;

// Stores generated PDFs in S3 (MinIO locally). The bucket and endpoint come from tdds.storage.s3.*.
public interface DocumentStorageService {

    // Saves the PDF under the key and returns the storage path to record on the job.
    // Throws StorageException (503) if the PDF could not be saved.
    String store(String key, byte[] pdf);

    // Opens the PDF stored under the key for streaming; the caller must close it.
    // DocumentMissingException (410) if there is no object, StorageException (503) if S3 fails.
    StoredDocument open(String key);
}
