package com.joshsoftware.templateDrivenDocumentService.engine.exception;

// Parent of all engine exceptions. Do not put customer data in the message.
public abstract class EngineException extends RuntimeException {

    protected EngineException(String detail) {
        super(detail);
    }

    protected EngineException(String detail, Throwable cause) {
        super(detail, cause);
    }
}
