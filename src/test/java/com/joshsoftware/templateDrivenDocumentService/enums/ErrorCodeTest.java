package com.joshsoftware.templateDrivenDocumentService.enums;

import com.joshsoftware.templateDrivenDocumentService.engine.exception.InvalidDefinitionException;
import com.joshsoftware.templateDrivenDocumentService.engine.exception.MappingException;
import com.joshsoftware.templateDrivenDocumentService.exception.DocumentUnavailableException;
import com.joshsoftware.templateDrivenDocumentService.exception.JobNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    // Broken template config is our bug, not the caller's: 500 MAPPING_ERROR
    @Test
    void brokenTemplateConfigIsReportedAsMappingError() {
        assertThat(ErrorCode.from(new InvalidDefinitionException("contract field 'ifsc': unknown type")))
                .isEqualTo(ErrorCode.MAPPING_ERROR);
        assertThat(ErrorCode.from(new MappingException("bad path"))).isEqualTo(ErrorCode.MAPPING_ERROR);
        assertThat(ErrorCode.MAPPING_ERROR.httpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Unknown job: 404, and a hostile path value is cut and stripped before it reaches the log message
    @Test
    void unknownJobIsNotFound() {
        JobNotFoundException ex = new JobNotFoundException("abc\r\nFAKE LOG LINE" + "x".repeat(100));

        assertThat(ErrorCode.from(ex)).isEqualTo(ErrorCode.JOB_NOT_FOUND);
        assertThat(ErrorCode.JOB_NOT_FOUND.httpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getMessage()).doesNotContain("\r", "\n", " LOG")
                .isEqualTo("No document job for jobId=abc??FAKE?LOG?LINExxxxxxxxxxxxxxxxxx");
    }

    // A job that exists but cannot be served: 409 while processing or failed, 410 once expired
    @Test
    void unavailableDocumentUsesTheGivenCode() {
        UUID jobId = UUID.randomUUID();

        assertThat(ErrorCode.from(new DocumentUnavailableException(ErrorCode.DOCUMENT_NOT_READY, jobId, JobStatus.PROCESSING)))
                .isEqualTo(ErrorCode.DOCUMENT_NOT_READY);
        assertThat(ErrorCode.DOCUMENT_NOT_READY.httpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorCode.DOCUMENT_FAILED.httpStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorCode.DOCUMENT_EXPIRED.httpStatus()).isEqualTo(HttpStatus.GONE);
        assertThat(new DocumentUnavailableException(ErrorCode.DOCUMENT_EXPIRED, jobId, JobStatus.EXPIRED).getMessage())
                .isEqualTo("Document unavailable: jobId=" + jobId + " status=EXPIRED");
    }
}
