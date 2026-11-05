package com.joshsoftware.templateDrivenDocumentService.exception;

import com.joshsoftware.templateDrivenDocumentService.enums.ErrorCode;
import lombok.Getter;

// Parent of all app exceptions. The ErrorCode decides the HTTP status and the client message.
// The detail message is only logged, never sent to the client. Do not put customer data in it.
@Getter
public abstract class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    protected AppException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    protected AppException(ErrorCode errorCode, String detail, Throwable cause) {
        super(detail, cause);
        this.errorCode = errorCode;
    }
}
