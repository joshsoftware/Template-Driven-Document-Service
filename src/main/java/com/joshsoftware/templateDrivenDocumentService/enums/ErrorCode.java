package com.joshsoftware.templateDrivenDocumentService.enums;

import org.springframework.http.HttpStatus;

/**
 * Every error the API can return, with its HTTP status and a fixed, client-safe message.
 * Messages never include request data (payload values may be account numbers or names).
 * Used only by the global exception handler to build {@code ErrorResponse}.
 **/
public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Request is invalid"),
    CONFIG_NOT_FOUND(HttpStatus.NOT_FOUND, "No active template found for the document type"),
    PAYLOAD_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "Request body exceeds the allowed size"),
    DATA_CONTRACT_VIOLATION(HttpStatus.UNPROCESSABLE_CONTENT, "Payload does not match the data contract"),
    MAPPING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Template mapping configuration is invalid"),
    RENDER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Document could not be rendered"),
    STORAGE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "Document could not be stored"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

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
}
