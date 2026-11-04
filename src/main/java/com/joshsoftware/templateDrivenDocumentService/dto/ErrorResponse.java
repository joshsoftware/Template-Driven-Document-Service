package com.joshsoftware.templateDrivenDocumentService.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.joshsoftware.templateDrivenDocumentService.enums.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

// Response body for every failed API call.
// Create it only with of(...), so the message and status always come from the ErrorCode.
// Empty fields are not sent in the JSON.
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String message;

    private Integer statusCode;

    private ErrorCode errorCode;

    private String requestId;

    // Only set after the job is created.
    private UUID jobId;

    // Only set when validation fails.
    private List<ViolationDTO> violations;

    // Error before the job is created.
    public static ErrorResponse of(ErrorCode code, String requestId) {
        return of(code, requestId, null, null);
    }

    // Error after the job is created.
    public static ErrorResponse of(ErrorCode code, String requestId, UUID jobId) {
        return of(code, requestId, jobId, null);
    }

    // Error with validation details.
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
