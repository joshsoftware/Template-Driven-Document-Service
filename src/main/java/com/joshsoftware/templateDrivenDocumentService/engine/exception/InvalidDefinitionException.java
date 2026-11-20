package com.joshsoftware.templateDrivenDocumentService.engine.exception;

// Template config (contract, mapping or date formats) is broken. A config bug, not a payload bug.
public class InvalidDefinitionException extends EngineException {

    public InvalidDefinitionException(String detail) {
        super(detail);
    }

    public InvalidDefinitionException(String detail, Throwable cause) {
        super(detail, cause);
    }
}
