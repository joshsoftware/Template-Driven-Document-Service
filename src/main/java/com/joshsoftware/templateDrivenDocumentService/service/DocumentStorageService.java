package com.joshsoftware.templateDrivenDocumentService.service;

// Stores generated PDFs. Stage 1 targets S3; the implementation in use is chosen by configuration.
public interface DocumentStorageService {

    // Saves the PDF under the key and returns the storage path to record on the job.
    // Throws StorageException (503) if the PDF could not be saved.
    String store(String key, byte[] pdf);
}
