package com.joshsoftware.templateDrivenDocumentService.exception;

import com.joshsoftware.templateDrivenDocumentService.enums.ErrorCode;

// Saving the PDF to S3 failed. Returns 503.
public class StorageException extends AppException {

    public StorageException(String detail, Throwable cause) {
        super(ErrorCode.STORAGE_ERROR, detail, cause);
    }
}
