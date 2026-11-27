package com.joshsoftware.templateDrivenDocumentService.service.impl;

import com.joshsoftware.templateDrivenDocumentService.annotation.Audit;
import com.joshsoftware.templateDrivenDocumentService.entity.DocumentJob;
import com.joshsoftware.templateDrivenDocumentService.enums.AuditAction;
import com.joshsoftware.templateDrivenDocumentService.enums.ErrorCode;
import com.joshsoftware.templateDrivenDocumentService.enums.JobStatus;
import com.joshsoftware.templateDrivenDocumentService.repository.DocumentJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// State changes only; the audit rows they trigger are covered by AuditAspectTest and DocumentGenerationServiceImplTest
class DocumentJobStateServiceImplTest {

    private static final UUID JOB_ID = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2024-06-01T04:30:00Z"), ZoneId.of("Asia/Kolkata"));

    private final DocumentJobRepository jobs = mock(DocumentJobRepository.class);
    private final DocumentJobStateServiceImpl service = new DocumentJobStateServiceImpl(jobs, CLOCK);
    private final DocumentJob job = new DocumentJob();

    @BeforeEach
    void setUp() {
        job.setId(JOB_ID);
        job.setStatus(JobStatus.PROCESSING);
        when(jobs.getReferenceById(JOB_ID)).thenReturn(job);
    }

    @Test
    void markReadySetsStatusPathAndCompletionTime() {
        service.markReady(JOB_ID, "MOBILE", "statement/v1/x.pdf");

        assertThat(job.getStatus()).isEqualTo(JobStatus.READY);
        assertThat(job.getStoragePath()).isEqualTo("statement/v1/x.pdf");
        // UTC, whatever the clock's zone
        assertThat(job.getCompletedAt()).isEqualTo(LocalDateTime.of(2024, 6, 1, 4, 30));
    }

    @Test
    void markFailedSetsStatusErrorCodeAndCompletionTime() {
        service.markFailed(JOB_ID, "MOBILE", ErrorCode.RENDER_ERROR);

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(job.getErrorCode()).isEqualTo("RENDER_ERROR");
        assertThat(job.getCompletedAt()).isEqualTo(LocalDateTime.of(2024, 6, 1, 4, 30));
    }

    @Test
    void eachTransitionDeclaresItsGenerateAudit() throws NoSuchMethodException {
        Audit ready = DocumentJobStateServiceImpl.class
                .getMethod("markReady", UUID.class, String.class, String.class).getAnnotation(Audit.class);
        Audit failed = DocumentJobStateServiceImpl.class
                .getMethod("markFailed", UUID.class, String.class, ErrorCode.class).getAnnotation(Audit.class);

        assertThat(ready.action()).isEqualTo(AuditAction.GENERATE);
        assertThat(ready.outcome()).isEqualTo(Audit.Outcome.SUCCESS);
        assertThat(ready.mode()).isEqualTo(Audit.Mode.SAME_TRANSACTION);
        assertThat(failed.action()).isEqualTo(AuditAction.GENERATE);
        assertThat(failed.outcome()).isEqualTo(Audit.Outcome.FAILURE);
        assertThat(failed.mode()).isEqualTo(Audit.Mode.SAME_TRANSACTION);
    }
}
