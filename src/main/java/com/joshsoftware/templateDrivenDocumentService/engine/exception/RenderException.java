package com.joshsoftware.templateDrivenDocumentService.engine.exception;

// PDF rendering failed. Do not log the cause message, it may contain customer data.
public class RenderException extends EngineException {

    public RenderException(String detail, Throwable cause) {
        super(detail, cause);
    }
}
