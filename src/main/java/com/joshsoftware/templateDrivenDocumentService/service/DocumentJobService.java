package com.joshsoftware.templateDrivenDocumentService.service;

import com.joshsoftware.templateDrivenDocumentService.dto.request.GenerateDocumentRequestDTO;
import com.joshsoftware.templateDrivenDocumentService.dto.response.DocumentJobResponseDTO;

// Assigns jobs to generate-document requests.
public interface DocumentJobService {

    // Creates a job for the request and returns its job id, request id and status.
    DocumentJobResponseDTO assignJob(GenerateDocumentRequestDTO request, String requestId);
}
