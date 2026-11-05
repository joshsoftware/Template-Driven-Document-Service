package com.joshsoftware.templateDrivenDocumentService.exception.handler;

import com.joshsoftware.templateDrivenDocumentService.constants.MdcConstants;
import com.joshsoftware.templateDrivenDocumentService.dto.ErrorResponse;
import com.joshsoftware.templateDrivenDocumentService.dto.ViolationDTO;
import com.joshsoftware.templateDrivenDocumentService.engine.exception.DataContractViolationException;
import com.joshsoftware.templateDrivenDocumentService.engine.exception.EngineException;
import com.joshsoftware.templateDrivenDocumentService.engine.exception.MappingException;
import com.joshsoftware.templateDrivenDocumentService.engine.exception.RenderException;
import com.joshsoftware.templateDrivenDocumentService.enums.ErrorCode;
import com.joshsoftware.templateDrivenDocumentService.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.UUID;

// Converts every exception into an ErrorResponse.
// Only our own exception messages are logged. Other messages may contain customer data,
// so only the exception type is logged and the stack trace goes to DEBUG.
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Payload does not match the contract. Returns 422 with the list of failed fields.
    @ExceptionHandler(DataContractViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataContractViolation(DataContractViolationException ex) {
        List<ViolationDTO> violations = ex.getViolations().stream()
                .map(v -> new ViolationDTO(v.getField(), v.getRule()))
                .toList();
        log.warn("[GlobalExceptionHandler#handleDataContractViolation] errorCode={} violations={}",
                ErrorCode.DATA_CONTRACT_VIOLATION, violations.size());
        return build(ErrorCode.DATA_CONTRACT_VIOLATION, violations);
    }

    // Mapping config is broken or PDF rendering failed. Returns 500.
    @ExceptionHandler({MappingException.class, RenderException.class})
    public ResponseEntity<ErrorResponse> handleEngineException(EngineException ex) {
        ErrorCode code = ErrorCode.from(ex);
        log.error("[GlobalExceptionHandler#handleEngineException] errorCode={} type={} causeType={}",
                code, ex.getClass().getSimpleName(), causeType(ex));
        log.debug("[GlobalExceptionHandler#handleEngineException] stack trace", ex);
        return build(code, null);
    }

    // Our own exceptions. Status comes from the error code.
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex) {
        ErrorCode code = ex.getErrorCode();
        if (code.httpStatus().is5xxServerError()) {
            log.error("[GlobalExceptionHandler#handleAppException] errorCode={} detail={} causeType={}",
                    code, ex.getMessage(), causeType(ex));
            log.debug("[GlobalExceptionHandler#handleAppException] stack trace", ex);
        } else {
            log.warn("[GlobalExceptionHandler#handleAppException] errorCode={} detail={}", code, ex.getMessage());
        }
        return build(code, null);
    }

    // Request validation failed. Returns 400 with the list of failed fields.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ViolationDTO> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ViolationDTO(fe.getField(), fe.getCode()))
                .toList();
        log.warn("[GlobalExceptionHandler#handleValidation] errorCode={} violations={}",
                ErrorCode.INVALID_REQUEST, violations.size());
        return build(ErrorCode.INVALID_REQUEST, violations);
    }

    // Body is not valid JSON or has an unknown value. Returns 400.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("[GlobalExceptionHandler#handleUnreadable] errorCode={} request body could not be read",
                ErrorCode.INVALID_REQUEST);
        return build(ErrorCode.INVALID_REQUEST, null);
    }

    // Unknown URL. Returns 404.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        log.warn("[GlobalExceptionHandler#handleNoResource] errorCode={} path={}",
                ErrorCode.NOT_FOUND, ex.getResourcePath());
        return build(ErrorCode.NOT_FOUND, null);
    }

    // Wrong HTTP method. Returns 405.
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("[GlobalExceptionHandler#handleMethodNotSupported] errorCode={} method={}",
                ErrorCode.METHOD_NOT_ALLOWED, ex.getMethod());
        return build(ErrorCode.METHOD_NOT_ALLOWED, null);
    }

    // Wrong content type. Returns 415.
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("[GlobalExceptionHandler#handleMediaTypeNotSupported] errorCode={} contentType={}",
                ErrorCode.UNSUPPORTED_MEDIA_TYPE, ex.getContentType());
        return build(ErrorCode.UNSUPPORTED_MEDIA_TYPE, null);
    }

    // Any other error. Returns 500.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("[GlobalExceptionHandler#handleUnexpected] errorCode={} type={} causeType={}",
                ErrorCode.INTERNAL_ERROR, ex.getClass().getName(), causeType(ex));
        log.debug("[GlobalExceptionHandler#handleUnexpected] stack trace", ex);
        return build(ErrorCode.INTERNAL_ERROR, null);
    }

    // Builds the error response with the request id and job id.
    private ResponseEntity<ErrorResponse> build(ErrorCode code, List<ViolationDTO> violations) {
        ErrorResponse body = ErrorResponse.of(code, MDC.get(MdcConstants.REQUEST_ID), jobIdFromMdc(), violations);
        return ResponseEntity.status(code.httpStatus()).body(body);
    }

    // Job id is only available after the job is created.
    private static UUID jobIdFromMdc() {
        String jobId = MDC.get(MdcConstants.JOB_ID);
        if (jobId == null) {
            return null;
        }
        try {
            return UUID.fromString(jobId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String causeType(Throwable ex) {
        return ex.getCause() == null ? null : ex.getCause().getClass().getName();
    }
}
