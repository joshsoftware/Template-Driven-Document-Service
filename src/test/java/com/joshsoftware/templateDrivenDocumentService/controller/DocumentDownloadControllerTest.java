package com.joshsoftware.templateDrivenDocumentService.controller;

import com.joshsoftware.templateDrivenDocumentService.constants.HeaderConstants;
import com.joshsoftware.templateDrivenDocumentService.dto.DocumentDownload;
import com.joshsoftware.templateDrivenDocumentService.dto.StoredDocument;
import com.joshsoftware.templateDrivenDocumentService.enums.ErrorCode;
import com.joshsoftware.templateDrivenDocumentService.enums.JobStatus;
import com.joshsoftware.templateDrivenDocumentService.exception.DocumentMissingException;
import com.joshsoftware.templateDrivenDocumentService.exception.DocumentUnavailableException;
import com.joshsoftware.templateDrivenDocumentService.exception.JobNotFoundException;
import com.joshsoftware.templateDrivenDocumentService.exception.StorageException;
import com.joshsoftware.templateDrivenDocumentService.exception.handler.GlobalExceptionHandler;
import com.joshsoftware.templateDrivenDocumentService.service.DocumentDownloadService;
import com.joshsoftware.templateDrivenDocumentService.util.filter.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Runs the controller with the real RequestIdFilter and GlobalExceptionHandler; the service is mocked
@WebMvcTest(DocumentDownloadController.class)
@Import({GlobalExceptionHandler.class, RequestIdFilter.class})
class DocumentDownloadControllerTest {

    private static final UUID JOB_ID = UUID.fromString("c2161c4d-7db2-421f-b8b4-68d3193b8ad2");
    private static final String URL = "/api/v1/documents/" + JOB_ID + "/download";
    private static final byte[] PDF = "%PDF-1.6 test document".getBytes();

    @MockitoBean
    private DocumentDownloadService documentDownloadService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void readyJobStreamsThePdfWithSafeHeaders() throws Exception {
        givenDownload(PDF.length);

        MvcResult started = mockMvc.perform(get(URL).header(HeaderConstants.X_REQUEST_ID, "req-1"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PDF))
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().longValue("Content-Length", PDF.length))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"interestcertificate.pdf\""))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string(HeaderConstants.X_REQUEST_ID, "req-1"));
    }

    @Test
    void unknownLengthSendsNoContentLength() throws Exception {
        givenDownload(-1);

        MvcResult started = mockMvc.perform(get(URL)).andExpect(request().asyncStarted()).andReturn();

        mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Content-Length"));
    }

    @Test
    void unknownJobIsNotFound() throws Exception {
        when(documentDownloadService.open(JOB_ID.toString())).thenThrow(new JobNotFoundException(JOB_ID.toString()));

        expectError(URL, 404, ErrorCode.JOB_NOT_FOUND);
    }

    // A String path variable: the service decides, so a bad id is our 404, not Spring's 400
    @Test
    void malformedIdReachesTheServiceAndIsNotFound() throws Exception {
        when(documentDownloadService.open("abc")).thenThrow(new JobNotFoundException("abc"));

        expectError("/api/v1/documents/abc/download", 404, ErrorCode.JOB_NOT_FOUND);
    }

    @Test
    void processingJobIsNotReady() throws Exception {
        givenRefusal(ErrorCode.DOCUMENT_NOT_READY, JobStatus.PROCESSING);

        expectError(URL, 409, ErrorCode.DOCUMENT_NOT_READY);
    }

    @Test
    void failedJobIsConflict() throws Exception {
        givenRefusal(ErrorCode.DOCUMENT_FAILED, JobStatus.FAILED);

        expectError(URL, 409, ErrorCode.DOCUMENT_FAILED);
    }

    @Test
    void expiredJobIsGone() throws Exception {
        givenRefusal(ErrorCode.DOCUMENT_EXPIRED, JobStatus.EXPIRED);

        expectError(URL, 410, ErrorCode.DOCUMENT_EXPIRED);
    }

    @Test
    void missingObjectIsGone() throws Exception {
        when(documentDownloadService.open(JOB_ID.toString())).thenThrow(new DocumentMissingException(null));

        expectError(URL, 410, ErrorCode.DOCUMENT_EXPIRED);
    }

    @Test
    void storageFailureIsServiceUnavailable() throws Exception {
        when(documentDownloadService.open(JOB_ID.toString()))
                .thenThrow(new StorageException("Could not read PDF from S3", null));

        expectError(URL, 503, ErrorCode.STORAGE_ERROR);
    }

    // Client gone mid-stream: swallowed in the controller, no 500 is attempted on a committed response
    @Test
    void brokenStreamIsSwallowed() throws Exception {
        givenDownload(PDF.length);
        doThrow(new IOException("Broken pipe")).when(documentDownloadService).stream(any(), any());

        MvcResult started = mockMvc.perform(get(URL)).andExpect(request().asyncStarted()).andReturn();

        mockMvc.perform(asyncDispatch(started)).andExpect(status().isOk());
        verify(documentDownloadService).stream(any(), any());
    }

    private void givenDownload(long length) throws IOException {
        StoredDocument stored = new StoredDocument(new ByteArrayInputStream(PDF), length);
        when(documentDownloadService.open(JOB_ID.toString()))
                .thenReturn(new DocumentDownload(JOB_ID, "interestcertificate.pdf", "MOBILE", stored));
        doAnswer(inv -> {
            OutputStream out = inv.getArgument(1);
            out.write(PDF);
            return null;
        }).when(documentDownloadService).stream(any(), any());
    }

    private void givenRefusal(ErrorCode code, JobStatus status) {
        when(documentDownloadService.open(JOB_ID.toString()))
                .thenThrow(new DocumentUnavailableException(code, JOB_ID, status));
    }

    private void expectError(String url, int httpStatus, ErrorCode code) throws Exception {
        mockMvc.perform(get(url).header(HeaderConstants.X_REQUEST_ID, "req-err"))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.errorCode").value(code.name()))
                .andExpect(jsonPath("$.message").value(code.defaultMessage()))
                .andExpect(jsonPath("$.requestId").value("req-err"));
    }
}
