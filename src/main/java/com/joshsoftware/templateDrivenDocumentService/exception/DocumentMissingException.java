package com.joshsoftware.templateDrivenDocumentService.exception;

import com.joshsoftware.templateDrivenDocumentService.enums.ErrorCode;

// The job says READY but its PDF is not in storage. Returns 410, same as an expired document.
// Fixed detail with no key: the handler logs it at WARN; the storage service logs the key at ERROR.
public class DocumentMissingException extends AppException {

    public DocumentMissingException(Throwable cause) {
        super(ErrorCode.DOCUMENT_EXPIRED, "Stored PDF is missing", cause);
    }
}
