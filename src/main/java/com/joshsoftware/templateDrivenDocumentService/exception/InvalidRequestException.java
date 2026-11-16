package com.joshsoftware.templateDrivenDocumentService.exception;

import com.joshsoftware.templateDrivenDocumentService.enums.ErrorCode;

// Request is invalid. Returns 400.
public class InvalidRequestException extends AppException {

    public InvalidRequestException(String detail) {
        super(ErrorCode.INVALID_REQUEST, detail);
    }
}
