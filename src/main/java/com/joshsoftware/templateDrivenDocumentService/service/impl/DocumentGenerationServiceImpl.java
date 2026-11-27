package com.joshsoftware.templateDrivenDocumentService.service.impl;

import com.joshsoftware.templateDrivenDocumentService.constants.MdcConstants;
import com.joshsoftware.templateDrivenDocumentService.dto.request.GenerateDocumentRequestDTO;
import com.joshsoftware.templateDrivenDocumentService.dto.response.GenerateDocumentResponseDTO;
import com.joshsoftware.templateDrivenDocumentService.engine.contract.ContractValidator;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.DocumentDefinition;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.DocumentDefinitionRegistry;
import com.joshsoftware.templateDrivenDocumentService.engine.mapping.MappingEngine;
import com.joshsoftware.templateDrivenDocumentService.engine.render.PdfRenderer;
import com.joshsoftware.templateDrivenDocumentService.engine.render.TemplateRenderer;
import com.joshsoftware.templateDrivenDocumentService.entity.DocumentJob;
import com.joshsoftware.templateDrivenDocumentService.enums.ErrorCode;
import com.joshsoftware.templateDrivenDocumentService.enums.JobStatus;
import com.joshsoftware.templateDrivenDocumentService.repository.DocumentJobRepository;
import com.joshsoftware.templateDrivenDocumentService.repository.TemplateRepository;
import com.joshsoftware.templateDrivenDocumentService.service.DocumentGenerationService;
import com.joshsoftware.templateDrivenDocumentService.service.DocumentJobStateService;
import com.joshsoftware.templateDrivenDocumentService.service.DocumentStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

// Sync Stage 1 flow:
//   1. registry.get(templateId)     - no ACTIVE template -> 404, broken config -> 500; no job yet (it needs a template row)
//   2. job row PROCESSING           - committed on its own, so the FAILED update below is never rolled back with it
//   3. map -> validate -> XHTML -> PDF
//   4. store the PDF
//   5. job READY + audit SUCCESS    - one transaction (DocumentJobStateService)
//   on any failure after 2: job FAILED + error code, audit FAILURE, then rethrow for GlobalExceptionHandler.
// The payload and the model hold customer data: they are never logged.
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentGenerationServiceImpl implements DocumentGenerationService {

    private static final String STORAGE_KEY_FORMAT = "%s/v%d/%s.pdf";

    private final DocumentDefinitionRegistry registry;
    private final MappingEngine mappingEngine;
    private final ContractValidator contractValidator;
    private final TemplateRenderer templateRenderer;
    private final PdfRenderer pdfRenderer;
    private final DocumentStorageService storageService;
    private final TemplateRepository templateRepository;
    private final DocumentJobRepository documentJobRepository;
    private final DocumentJobStateService jobStateService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    @Override
    public GenerateDocumentResponseDTO generate(GenerateDocumentRequestDTO request, String requestId) {
        long start = System.currentTimeMillis();
        log.info("[DocumentGenerationServiceImpl#generate] start templateId={} channel={}",
                request.getTemplateId(), request.getChannel());

        DocumentDefinition definition = registry.get(request.getTemplateId());
        String channel = request.getChannel().name();
        UUID jobId = createJob(definition, channel, requestId);
        // From here on, error responses and log lines carry the job id
        MDC.put(MdcConstants.JOB_ID, jobId.toString());

        String storagePath;
        try {
            byte[] pdf = render(definition, request.getPayload());
            storagePath = storageService.store(storageKey(definition, jobId), pdf);
            log.debug("[DocumentGenerationServiceImpl#generate] stored bytes={}", pdf.length);
            jobStateService.markReady(jobId, channel, storagePath);
        } catch (RuntimeException ex) {
            markFailed(jobId, channel, ErrorCode.from(ex), ex);
            throw ex;
        }
        
        log.info("[DocumentGenerationServiceImpl#generate] end templateId={} version={} status={} durationMs={}",
                definition.templateId(), definition.version(), JobStatus.READY, System.currentTimeMillis() - start);
        return new GenerateDocumentResponseDTO(jobId, JobStatus.READY);
    }

    // The four engine steps, in order. Each throws its own EngineException, mapped to an ErrorCode.
    private byte[] render(DocumentDefinition definition, JsonNode payload) {
        JsonNode mapped = mappingEngine.transform(definition, payload);
        Map<String, Object> model = contractValidator.validate(definition, mapped);
        String xhtml = templateRenderer.renderXhtml(definition, model);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pdfRenderer.render(xhtml, out);
        return out.toByteArray();
    }

    private UUID createJob(DocumentDefinition definition, String channel, String requestId) {
        DocumentJob job = new DocumentJob();
        job.setId(UUID.randomUUID());
        job.setChannel(channel);
        job.setRequestId(requestId);
        job.setStatus(JobStatus.PROCESSING);
        job.setCreatedAt(now());
        transactionTemplate.executeWithoutResult(status -> {
            job.setTemplate(templateRepository.getReferenceById(definition.templateVersionId()));
            documentJobRepository.save(job);
        });
        return job.getId();
    }

    // Must not hide the original error: if recording the failure also fails, log it and keep the first one
    private void markFailed(UUID jobId, String channel, ErrorCode errorCode, RuntimeException cause) {
        try {
            jobStateService.markFailed(jobId, channel, errorCode);
            log.warn("[DocumentGenerationServiceImpl#markFailed] job failed errorCode={}", errorCode);
        } catch (RuntimeException recordError) {
            log.error("[DocumentGenerationServiceImpl#markFailed] could not mark job FAILED errorCode={} type={}",
                    errorCode, recordError.getClass().getName());
            cause.addSuppressed(recordError);
        }
    }

    private static String storageKey(DocumentDefinition definition, UUID jobId) {
        return STORAGE_KEY_FORMAT.formatted(definition.templateId(), definition.version(), jobId);
    }

    // Timestamp columns are TIMESTAMPTZ written as UTC (hibernate.jdbc.time_zone), whatever the engine zone is
    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
