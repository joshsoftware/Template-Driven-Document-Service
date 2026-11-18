package com.joshsoftware.templateDrivenDocumentService.service.impl;

import com.joshsoftware.templateDrivenDocumentService.constants.MdcConstants;
import com.joshsoftware.templateDrivenDocumentService.dto.request.GenerateDocumentRequestDTO;
import com.joshsoftware.templateDrivenDocumentService.dto.response.DocumentJobResponseDTO;
import com.joshsoftware.templateDrivenDocumentService.enums.JobStatus;
import com.joshsoftware.templateDrivenDocumentService.service.DocumentJobService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.UUID;

// Creates document jobs. Nothing is stored until the document_jobs migration is merged.
@Slf4j
@Service
public class DocumentJobServiceImpl implements DocumentJobService {

    private static final String JOB_ACCEPTED_MESSAGE = "Document generation request accepted";

    // Creates a job in PROCESSING status and returns its job id, request id and status.
    @Override
    public DocumentJobResponseDTO assignJob(GenerateDocumentRequestDTO request, String requestId) {
        UUID jobId = UUID.randomUUID();
        MDC.put(MdcConstants.JOB_ID, jobId.toString());
        DocumentJobResponseDTO job =
                new DocumentJobResponseDTO(JOB_ACCEPTED_MESSAGE, jobId, requestId, JobStatus.PROCESSING);

        log.info("[DocumentJobServiceImpl#assignJob] job assigned templateId={} channel={} status={}",
                request.getTemplateId(), request.getChannel(), job.getStatus());
        return job;
    }
}
