package com.joshsoftware.templateDrivenDocumentService.service;

// Stores generated PDFs in S3 (MinIO locally). The bucket and endpoint come from tdds.storage.s3.*.
public interface DocumentStorageService {

    // Saves the PDF under the key and returns the storage path to record on the job.
    // Throws StorageException (503) if the PDF could not be saved.
    String store(String key, byte[] pdf);
}
