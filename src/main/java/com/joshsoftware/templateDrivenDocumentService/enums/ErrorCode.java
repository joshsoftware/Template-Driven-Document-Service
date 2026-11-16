package com.joshsoftware.templateDrivenDocumentService.enums;

import com.joshsoftware.templateDrivenDocumentService.engine.exception.DataContractViolationException;
import com.joshsoftware.templateDrivenDocumentService.engine.exception.MappingException;
import com.joshsoftware.templateDrivenDocumentService.engine.exception.RenderException;
import com.joshsoftware.templateDrivenDocumentService.exception.AppException;
import org.springframework.http.HttpStatus;

// All errors the API can return, each with its HTTP status and a fixed message.
// Messages never contain request data.
public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Request is invalid"),
    CONFIG_NOT_FOUND(HttpStatus.NOT_FOUND, "No active template found for the document type"),
    PAYLOAD_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "Request body exceeds the allowed size"),
    DATA_CONTRACT_VIOLATION(HttpStatus.UNPROCESSABLE_CONTENT, "Payload does not match the data contract"),
    MAPPING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Template mapping configuration is invalid"),
    RENDER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Document could not be rendered"),
    STORAGE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "Document could not be stored"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"),

    // Wrong URL, method or content type
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content type not supported");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    // Finds the error code for an exception. Unknown exceptions give INTERNAL_ERROR.
    public static ErrorCode from(Throwable ex) {
        return switch (ex) {
            case AppException appException -> appException.getErrorCode();
            case DataContractViolationException ignored -> DATA_CONTRACT_VIOLATION;
            case MappingException ignored -> MAPPING_ERROR;
            case RenderException ignored -> RENDER_ERROR;
            default -> INTERNAL_ERROR;
        };
    }
}
