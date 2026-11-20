package com.joshsoftware.templateDrivenDocumentService.dto.response;

import com.joshsoftware.templateDrivenDocumentService.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

// Response body for POST /api/v1/generate-document.
@Getter
@AllArgsConstructor
public class DocumentJobResponseDTO {

    private String message;

    private UUID jobId;

    // Request id of this API call.
    private String requestId;

    private JobStatus status;
}
