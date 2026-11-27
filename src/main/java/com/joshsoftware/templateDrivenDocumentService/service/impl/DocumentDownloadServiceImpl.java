package com.joshsoftware.templateDrivenDocumentService.service.impl;

import com.joshsoftware.templateDrivenDocumentService.annotation.Audit;
import com.joshsoftware.templateDrivenDocumentService.dto.DocumentDownload;
import com.joshsoftware.templateDrivenDocumentService.dto.JobDownloadView;
import com.joshsoftware.templateDrivenDocumentService.dto.StoredDocument;
import com.joshsoftware.templateDrivenDocumentService.enums.AuditAction;
import com.joshsoftware.templateDrivenDocumentService.enums.ErrorCode;
import com.joshsoftware.templateDrivenDocumentService.exception.DocumentUnavailableException;
import com.joshsoftware.templateDrivenDocumentService.exception.JobNotFoundException;
import com.joshsoftware.templateDrivenDocumentService.repository.DocumentJobRepository;
import com.joshsoftware.templateDrivenDocumentService.service.DocumentDownloadService;
import com.joshsoftware.templateDrivenDocumentService.service.DocumentStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

// Serves READY PDFs from storage. Refused calls (404/409/410) are logged, not audited; every stream attempt is audited.
// Logs show job id, status and sizes only: never the storage path at INFO or any PDF content.
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentDownloadServiceImpl implements DocumentDownloadService {

    private static final String FILE_NAME_FORMAT = "%s.pdf";
    private static final int BUFFER_BYTES = 64 * 1024;

    private final DocumentJobRepository documentJobRepository;
    private final DocumentStorageService storageService;

    @Override
    public DocumentDownload open(String jobId) {
        UUID id = parse(jobId);
        JobDownloadView job = documentJobRepository.findDownloadView(id)
                .orElseThrow(() -> new JobNotFoundException(jobId));
        checkDownloadable(job);
        StoredDocument document = storageService.open(job.storagePath());
        log.info("[DocumentDownloadServiceImpl#open] opened jobId={} bytes={}", id, document.contentLength());
        return new DocumentDownload(id, FILE_NAME_FORMAT.formatted(job.templateId()), job.channel(), document);
    }

    // Audited after streaming in its own transaction: SUCCESS when it returns, FAILURE when it throws
    @Override
    @Audit(action = AuditAction.DOWNLOAD, jobId = "#download.jobId()", channel = "#download.channel()",
            mode = Audit.Mode.SEPARATE)
    public void stream(DocumentDownload download, OutputStream out) throws IOException {
        long start = System.currentTimeMillis();
        long bytes;
        try (StoredDocument document = download.document()) {
            bytes = copy(document.content(), out);
            out.flush();
        } catch (IOException e) {
            // Client went away or storage broke mid-stream: the job is unchanged, so the client can retry
            log.warn("[DocumentDownloadServiceImpl#stream] stream broken jobId={} type={}",
                    download.jobId(), e.getClass().getName());
            throw e;
        }
        log.info("[DocumentDownloadServiceImpl#stream] streamed jobId={} bytes={} durationMs={}",
                download.jobId(), bytes, System.currentTimeMillis() - start);
    }

    // Fixed 64 KB chunks: memory stays flat whatever the PDF size
    private static long copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_BYTES];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            total += read;
        }
        return total;
    }

    // A path value that is not a UUID can never be a job: same 404 as an unknown id
    private static UUID parse(String jobId) {
        try {
            return UUID.fromString(jobId);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new JobNotFoundException(jobId);
        }
    }

    // Only READY with a stored PDF is served; everything else fails closed
    private static void checkDownloadable(JobDownloadView job) {
        ErrorCode refusal = switch (job.status()) {
            case PROCESSING -> ErrorCode.DOCUMENT_NOT_READY;
            case FAILED -> ErrorCode.DOCUMENT_FAILED;
            case EXPIRED -> ErrorCode.DOCUMENT_EXPIRED;
            case READY -> null;
        };
        if (refusal == null && (job.storagePath() == null || job.storagePath().isBlank())) {
            log.error("[DocumentDownloadServiceImpl#checkDownloadable] READY job has no storage path jobId={}", job.jobId());
            refusal = ErrorCode.DOCUMENT_EXPIRED;
        }
        if (refusal != null) {
            throw new DocumentUnavailableException(refusal, job.jobId(), job.status());
        }
    }
}
