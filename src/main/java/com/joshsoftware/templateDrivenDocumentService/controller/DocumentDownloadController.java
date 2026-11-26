package com.joshsoftware.templateDrivenDocumentService.controller;

import com.joshsoftware.templateDrivenDocumentService.dto.DocumentDownload;
import com.joshsoftware.templateDrivenDocumentService.service.DocumentDownloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.Map;

// Download API: streams a READY job's PDF from storage.
// Refusals (404/409/410/503) are thrown by open() before the body starts, so GlobalExceptionHandler
// returns a normal ErrorResponse. jobId is a String so a malformed id gets our 404, not Spring's 400.
@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentDownloadController {

    private static final String NO_CACHE = "no-cache";
    private static final String NO_SNIFF = "nosniff";
    private static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";

    private final DocumentDownloadService documentDownloadService;

    @GetMapping("/{jobId}/download")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable String jobId) {
        long start = System.currentTimeMillis();
        DocumentDownload download = documentDownloadService.open(jobId);
        log.info("[DocumentDownloadController#download] start jobId={}", download.jobId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        if (download.document().contentLength() >= 0) {
            headers.setContentLength(download.document().contentLength());
        }
        headers.setContentDisposition(ContentDisposition.attachment().filename(download.fileName()).build());
        headers.setCacheControl(CacheControl.noStore());
        headers.setPragma(NO_CACHE);
        headers.set(X_CONTENT_TYPE_OPTIONS, NO_SNIFF);

        // The body is written on another thread: carry the request id over so its logs stay traceable
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        StreamingResponseBody body = out -> {
            if (mdc != null) {
                MDC.setContextMap(mdc);
            }
            try {
                documentDownloadService.stream(download, out);
                log.info("[DocumentDownloadController#download] end jobId={} durationMs={}",
                        download.jobId(), System.currentTimeMillis() - start);
            } catch (IOException e) {
                // Client went away mid-stream; already logged and audited, and there is no one to answer
            } finally {
                MDC.clear();
            }
        };
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
