package com.joshsoftware.templateDrivenDocumentService.engine.exception;

// Mapping config is broken, for example a bad path. This is a config bug, not a payload bug.
public class MappingException extends EngineException {

    public MappingException(String detail) {
        super(detail);
    }

    public MappingException(String detail, Throwable cause) {
        super(detail, cause);
    }
}
