package com.joshsoftware.templateDrivenDocumentService.dto.response;

import com.joshsoftware.templateDrivenDocumentService.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

// Data of a successful POST /api/v1/generate-document. The PDF itself is not returned, only the job.
@Getter
@Builder
@AllArgsConstructor
public class GenerateDocumentResponseDTO {

    private UUID jobId;

    // READY in Stage 1: the call is sync and only returns once the PDF is stored
    private JobStatus status;
}
