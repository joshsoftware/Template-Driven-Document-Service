package com.joshsoftware.templateDrivenDocumentService.service;

import com.joshsoftware.templateDrivenDocumentService.dto.request.GenerateDocumentRequestDTO;
import com.joshsoftware.templateDrivenDocumentService.dto.response.GenerateDocumentResponseDTO;

// Generates a document synchronously: map, validate, render, store, and record the job and audit rows.
public interface DocumentGenerationService {

    // Returns the job once the PDF is stored (status READY). On failure after the job is created,
    // the job is marked FAILED with the error code, a FAILURE audit row is written, and the error is rethrown.
    GenerateDocumentResponseDTO generate(GenerateDocumentRequestDTO request, String requestId);
}
