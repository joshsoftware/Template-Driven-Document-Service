package com.joshsoftware.templateDrivenDocumentService.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.joshsoftware.templateDrivenDocumentService.enums.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * Error response returned by the API, with a fixed, client-safe message and optional details.
 * Messages never include request data (payload values may be account numbers or names).
 * Built only by the global exception handler. Shares {@code message} and {@code statusCode}
 * with {@link ApiResponse}, so clients read both the same way.
 *
 * <p>Constructor and builder are private: create it only through {@code of(...)}, so
 * {@code message} and {@code statusCode} always come from the {@link ErrorCode}.
 **/
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String message;

    private Integer statusCode;

    private ErrorCode errorCode;

    private String requestId;

    /** Present only when the job row exists. */
    private UUID jobId;

    /** Present only for validation failures. */
    private List<ViolationDTO> violations;

    /** Failure before a job row exists (bad request, unknown doc type). */
    public static ErrorResponse of(ErrorCode code, String requestId) {
        return of(code, requestId, null, null);
    }

    /** Failure after the job row exists. */
    public static ErrorResponse of(ErrorCode code, String requestId, UUID jobId) {
        return of(code, requestId, jobId, null);
    }

    /** Validation failure, with or without a job row. */
    public static ErrorResponse of(ErrorCode code, String requestId, UUID jobId, List<ViolationDTO> violations) {
        return ErrorResponse.builder()
                .message(code.defaultMessage())
                .statusCode(code.httpStatus().value())
                .errorCode(code)
                .requestId(requestId)
                .jobId(jobId)
                .violations(violations == null || violations.isEmpty() ? null : List.copyOf(violations))
                .build();
    }
}
