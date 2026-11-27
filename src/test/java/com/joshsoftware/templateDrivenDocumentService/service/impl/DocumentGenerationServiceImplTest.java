package com.joshsoftware.templateDrivenDocumentService.service.impl;

import com.joshsoftware.templateDrivenDocumentService.audit.AuditAspect;
import com.joshsoftware.templateDrivenDocumentService.constants.MdcConstants;
import com.joshsoftware.templateDrivenDocumentService.dto.request.GenerateDocumentRequestDTO;
import com.joshsoftware.templateDrivenDocumentService.dto.response.GenerateDocumentResponseDTO;
import com.joshsoftware.templateDrivenDocumentService.engine.contract.DefaultContractValidator;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.DocumentDefinition;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.DocumentDefinitionRegistry;
import com.joshsoftware.templateDrivenDocumentService.engine.exception.DataContractViolationException;
import com.joshsoftware.templateDrivenDocumentService.engine.mapping.DefaultMappingEngine;
import com.joshsoftware.templateDrivenDocumentService.engine.render.OpenHtmlPdfRenderer;
import com.joshsoftware.templateDrivenDocumentService.engine.render.ThymeleafTemplateRenderer;
import com.joshsoftware.templateDrivenDocumentService.entity.AuditLog;
import com.joshsoftware.templateDrivenDocumentService.entity.DocumentJob;
import com.joshsoftware.templateDrivenDocumentService.entity.Template;
import com.joshsoftware.templateDrivenDocumentService.enums.AuditAction;
import com.joshsoftware.templateDrivenDocumentService.enums.AuditOutcome;
import com.joshsoftware.templateDrivenDocumentService.enums.Channel;
import com.joshsoftware.templateDrivenDocumentService.enums.JobStatus;
import com.joshsoftware.templateDrivenDocumentService.exception.ConfigNotFoundException;
import com.joshsoftware.templateDrivenDocumentService.exception.StorageException;
import com.joshsoftware.templateDrivenDocumentService.repository.AuditLogRepository;
import com.joshsoftware.templateDrivenDocumentService.repository.DocumentJobRepository;
import com.joshsoftware.templateDrivenDocumentService.repository.TemplateRepository;
import com.joshsoftware.templateDrivenDocumentService.service.DocumentJobStateService;
import com.joshsoftware.templateDrivenDocumentService.service.DocumentStorageService;
import com.joshsoftware.templateDrivenDocumentService.support.Fixtures;
import com.joshsoftware.templateDrivenDocumentService.support.PdfText;
import com.joshsoftware.templateDrivenDocumentService.support.StatementDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Real engine, job state service and @Audit aspect on the statement fixtures; only the registry, storage and repositories are mocked
class DocumentGenerationServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2024-06-01T04:30:00Z"), ZoneId.of("Asia/Kolkata"));
    private static final String REQUEST_ID = "req-1";

    private final DocumentDefinition statement = StatementDefinition.load();
    private final DocumentDefinitionRegistry registry = mock(DocumentDefinitionRegistry.class);
    private final DocumentStorageService storage = mock(DocumentStorageService.class);
    private final TemplateRepository templates = mock(TemplateRepository.class);
    private final DocumentJobRepository jobs = mock(DocumentJobRepository.class);
    private final AuditLogRepository audits = mock(AuditLogRepository.class);

    // Stands in for the database: jobs by id, audit rows in insert order
    private final Map<UUID, DocumentJob> savedJobs = new HashMap<>();
    private final List<AuditLog> savedAudits = new ArrayList<>();

    private DocumentGenerationServiceImpl service;

    @BeforeEach
    void setUp() {
        // A mock transaction manager runs each callback directly, with no real transaction
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        service = new DocumentGenerationServiceImpl(registry, new DefaultMappingEngine(CLOCK),
                new DefaultContractValidator(), new ThymeleafTemplateRenderer(), new OpenHtmlPdfRenderer(),
                storage, templates, jobs, auditedJobStateService(transactionManager), tx, CLOCK);

        when(registry.get("statement")).thenReturn(statement);
        Template template = new Template();
        template.setId(statement.templateVersionId());
        when(templates.getReferenceById(statement.templateVersionId())).thenReturn(template);
        when(jobs.save(any(DocumentJob.class))).thenAnswer(inv -> {
            DocumentJob job = inv.getArgument(0);
            savedJobs.put(job.getId(), job);
            return job;
        });
        when(jobs.getReferenceById(any(UUID.class))).thenAnswer(inv -> savedJobs.get(inv.<UUID>getArgument(0)));
        when(audits.save(any(AuditLog.class))).thenAnswer(inv -> {
            savedAudits.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(storage.store(anyString(), any(byte[].class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generatesStoresAndMarksTheJobReady() throws IOException {
        GenerateDocumentResponseDTO response = service.generate(request("samples/payload-complete.json"), REQUEST_ID);

        assertThat(response.getStatus()).isEqualTo(JobStatus.READY);
        DocumentJob job = savedJobs.get(response.getJobId());
        assertThat(job.getStatus()).isEqualTo(JobStatus.READY);
        assertThat(job.getRequestId()).isEqualTo(REQUEST_ID);
        assertThat(job.getChannel()).isEqualTo("MOBILE");
        assertThat(job.getTemplate().getId()).isEqualTo(statement.templateVersionId());
        assertThat(job.getStoragePath()).isEqualTo("statement/v1/" + response.getJobId() + ".pdf");
        assertThat(job.getErrorCode()).isNull();
        // DB timestamps are UTC even though the engine clock is in IST
        assertThat(job.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 6, 1, 4, 30));
        assertThat(job.getCompletedAt()).isNotNull();
        assertThat(MDC.get(MdcConstants.JOB_ID)).isEqualTo(response.getJobId().toString());

        assertThat(savedAudits).singleElement().satisfies(audit -> {
            assertThat(audit.getAction()).isEqualTo(AuditAction.GENERATE);
            assertThat(audit.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
            assertThat(audit.getDocumentJob()).isSameAs(job);
            assertThat(audit.getChannel()).isEqualTo("MOBILE");
        });

        ArgumentCaptor<byte[]> pdf = ArgumentCaptor.forClass(byte[].class);
        verify(storage).store(eq(job.getStoragePath()), pdf.capture());
        assertThat(PdfText.of(pdf.getValue()).text()).contains("TEST CUSTOMER");
    }

    @Test
    void contractViolationMarksTheJobFailedAndRethrows() {
        assertThatThrownBy(() -> service.generate(request("samples/payload.json"), REQUEST_ID))
                .isInstanceOf(DataContractViolationException.class);

        DocumentJob job = onlyJob();
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorCode()).isEqualTo("DATA_CONTRACT_VIOLATION");
        assertThat(job.getStoragePath()).isNull();
        assertThat(job.getCompletedAt()).isNotNull();
        assertThat(savedAudits).singleElement()
                .extracting(AuditLog::getOutcome).isEqualTo(AuditOutcome.FAILURE);
        verifyNoInteractions(storage);
        // GlobalExceptionHandler reads it to put the jobId in the error response
        assertThat(MDC.get(MdcConstants.JOB_ID)).isEqualTo(job.getId().toString());
    }

    @Test
    void storageFailureMarksTheJobFailedWithStorageError() {
        when(storage.store(anyString(), any(byte[].class)))
                .thenThrow(new StorageException("bucket unavailable", null));

        assertThatThrownBy(() -> service.generate(request("samples/payload-complete.json"), REQUEST_ID))
                .isInstanceOf(StorageException.class);

        DocumentJob job = onlyJob();
        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorCode()).isEqualTo("STORAGE_ERROR");
        assertThat(savedAudits).singleElement()
                .extracting(AuditLog::getOutcome).isEqualTo(AuditOutcome.FAILURE);
    }

    @Test
    void failureToRecordTheFailureKeepsTheOriginalError() {
        when(audits.save(any(AuditLog.class))).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.generate(request("samples/payload.json"), REQUEST_ID))
                .isInstanceOf(DataContractViolationException.class)
                .satisfies(e -> assertThat(e.getSuppressed()).singleElement()
                        .isInstanceOf(IllegalStateException.class));
    }

    @Test
    void unknownTemplateCreatesNoJob() {
        when(registry.get("statement")).thenThrow(new ConfigNotFoundException("statement"));

        assertThatThrownBy(() -> service.generate(request("samples/payload-complete.json"), REQUEST_ID))
                .isInstanceOf(ConfigNotFoundException.class);

        verify(jobs, never()).save(any());
        verifyNoInteractions(audits, storage);
        assertThat(MDC.get(MdcConstants.JOB_ID)).isNull();
    }

    // Real state service behind the real @Audit aspect, so audit rows are written as in the app
    private DocumentJobStateService auditedJobStateService(PlatformTransactionManager transactionManager) {
        AspectJProxyFactory factory = new AspectJProxyFactory(new DocumentJobStateServiceImpl(jobs, CLOCK));
        factory.setProxyTargetClass(true);
        factory.addAspect(new AuditAspect(new AuditLogServiceImpl(audits), jobs, transactionManager, CLOCK));
        return factory.getProxy();
    }

    private DocumentJob onlyJob() {
        assertThat(savedJobs).hasSize(1);
        return savedJobs.values().iterator().next();
    }

    private static GenerateDocumentRequestDTO request(String payloadFixture) {
        GenerateDocumentRequestDTO request = new GenerateDocumentRequestDTO();
        request.setTemplateId("statement");
        request.setChannel(Channel.MOBILE);
        request.setPayload(Fixtures.json(payloadFixture));
        return request;
    }
}
