package com.joshsoftware.templateDrivenDocumentService.service.impl;

import com.joshsoftware.templateDrivenDocumentService.dto.DocumentDownload;
import com.joshsoftware.templateDrivenDocumentService.dto.JobDownloadView;
import com.joshsoftware.templateDrivenDocumentService.dto.StoredDocument;
import com.joshsoftware.templateDrivenDocumentService.entity.DocumentJob;
import com.joshsoftware.templateDrivenDocumentService.enums.AuditAction;
import com.joshsoftware.templateDrivenDocumentService.enums.AuditOutcome;
import com.joshsoftware.templateDrivenDocumentService.enums.ErrorCode;
import com.joshsoftware.templateDrivenDocumentService.enums.JobStatus;
import com.joshsoftware.templateDrivenDocumentService.exception.DocumentUnavailableException;
import com.joshsoftware.templateDrivenDocumentService.exception.JobNotFoundException;
import com.joshsoftware.templateDrivenDocumentService.repository.DocumentJobRepository;
import com.joshsoftware.templateDrivenDocumentService.service.AuditLogService;
import com.joshsoftware.templateDrivenDocumentService.service.DocumentStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Mocked repository, storage and audit; real streaming against in-memory streams
class DocumentDownloadServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2024-06-01T04:30:00Z"), ZoneId.of("Asia/Kolkata"));
    private static final LocalDateTime NOW_UTC = LocalDateTime.of(2024, 6, 1, 4, 30);
    private static final UUID JOB_ID = UUID.fromString("c2161c4d-7db2-421f-b8b4-68d3193b8ad2");
    private static final String PATH = "interestcertificate/v1/" + JOB_ID + ".pdf";
    private static final byte[] PDF = "%PDF-1.6 test document".getBytes();

    private final DocumentJobRepository jobs = mock(DocumentJobRepository.class);
    private final DocumentStorageService storage = mock(DocumentStorageService.class);
    private final AuditLogService audit = mock(AuditLogService.class);
    private final DocumentJob jobRef = new DocumentJob();
    private DocumentDownloadServiceImpl service;

    @BeforeEach
    void setUp() {
        TransactionTemplate tx = new TransactionTemplate(mock(PlatformTransactionManager.class));
        service = new DocumentDownloadServiceImpl(jobs, storage, audit, tx, CLOCK);
        when(jobs.getReferenceById(JOB_ID)).thenReturn(jobRef);
    }

    @Test
    void malformedIdIsNotFound() {
        assertThatThrownBy(() -> service.open("not-a-uuid"))
                .isInstanceOf(JobNotFoundException.class);
        verifyNoInteractions(jobs, storage, audit);
    }

    @Test
    void unknownIdIsNotFound() {
        when(jobs.findDownloadView(JOB_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.open(JOB_ID.toString()))
                .isInstanceOf(JobNotFoundException.class);
        verifyNoInteractions(storage, audit);
    }

    @ParameterizedTest
    @CsvSource({"PROCESSING, DOCUMENT_NOT_READY", "FAILED, DOCUMENT_FAILED", "EXPIRED, DOCUMENT_EXPIRED"})
    void jobThatIsNotReadyIsRefused(JobStatus status, ErrorCode expected) {
        when(jobs.findDownloadView(JOB_ID)).thenReturn(Optional.of(view(status, PATH)));

        assertThatThrownBy(() -> service.open(JOB_ID.toString()))
                .isInstanceOf(DocumentUnavailableException.class)
                .extracting("errorCode").isEqualTo(expected);
        verifyNoInteractions(storage, audit);
    }

    @Test
    void readyJobWithoutAStoragePathIsExpired() {
        when(jobs.findDownloadView(JOB_ID)).thenReturn(Optional.of(view(JobStatus.READY, null)));

        assertThatThrownBy(() -> service.open(JOB_ID.toString()))
                .isInstanceOf(DocumentUnavailableException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DOCUMENT_EXPIRED);
        verifyNoInteractions(storage, audit);
    }

    @Test
    void readyJobOpensItsPdf() {
        StoredDocument stored = new StoredDocument(new ByteArrayInputStream(PDF), PDF.length);
        when(jobs.findDownloadView(JOB_ID)).thenReturn(Optional.of(view(JobStatus.READY, PATH)));
        when(storage.open(PATH)).thenReturn(stored);

        DocumentDownload download = service.open(JOB_ID.toString());

        assertThat(download.jobId()).isEqualTo(JOB_ID);
        assertThat(download.fileName()).isEqualTo("interestcertificate.pdf");
        assertThat(download.channel()).isEqualTo("MOBILE");
        assertThat(download.document()).isSameAs(stored);
        verifyNoInteractions(audit);
    }

    @Test
    void streamCopiesTheBytesClosesAndAuditsSuccess() throws IOException {
        TrackingStream in = new TrackingStream(PDF);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        service.stream(download(in), out);

        assertThat(out.toByteArray()).isEqualTo(PDF);
        assertThat(in.closed).isTrue();
        verify(audit).record(jobRef, AuditAction.DOWNLOAD, AuditOutcome.SUCCESS, "MOBILE", NOW_UTC);
    }

    // Larger than one 64 KB chunk, so the copy loop runs several times
    @Test
    void streamCopiesALargePdfExactly() throws IOException {
        byte[] large = new byte[200_000];
        for (int i = 0; i < large.length; i++) {
            large[i] = (byte) i;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        service.stream(download(new TrackingStream(large)), out);

        assertThat(out.toByteArray()).isEqualTo(large);
    }

    @Test
    void brokenStreamAuditsFailureRethrowsAndStillCloses() {
        TrackingStream in = new TrackingStream(PDF);
        OutputStream clientGone = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("Broken pipe");
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                throw new IOException("Broken pipe");
            }
        };

        assertThatThrownBy(() -> service.stream(download(in), clientGone))
                .isInstanceOf(IOException.class)
                .hasMessage("Broken pipe");
        assertThat(in.closed).isTrue();
        verify(audit).record(jobRef, AuditAction.DOWNLOAD, AuditOutcome.FAILURE, "MOBILE", NOW_UTC);
    }

    // The client already has the PDF: a failed audit write is logged, not thrown
    @Test
    void auditFailureDoesNotFailTheDownload() {
        doThrow(new IllegalStateException("db down"))
                .when(audit).record(any(), any(), any(), any(), any());

        assertThatCode(() -> service.stream(download(new TrackingStream(PDF)), new ByteArrayOutputStream()))
                .doesNotThrowAnyException();
    }

    private static JobDownloadView view(JobStatus status, String path) {
        return new JobDownloadView(JOB_ID, status, path, "MOBILE", "interestcertificate");
    }

    private static DocumentDownload download(TrackingStream in) {
        return new DocumentDownload(JOB_ID, "interestcertificate.pdf", "MOBILE", new StoredDocument(in, -1));
    }

    // Records whether the stream was closed, i.e. the S3 connection released
    private static final class TrackingStream extends ByteArrayInputStream {
        private boolean closed;

        TrackingStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
