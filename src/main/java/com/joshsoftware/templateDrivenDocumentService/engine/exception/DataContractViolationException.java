package com.joshsoftware.templateDrivenDocumentService.engine.exception;

import com.joshsoftware.templateDrivenDocumentService.engine.contract.Violation;
import lombok.Getter;

import java.util.List;

// Payload does not match the data contract. Holds all failed checks.
@Getter
public class DataContractViolationException extends EngineException {

    private final List<Violation> violations;

    public DataContractViolationException(List<Violation> violations) {
        super("%d data contract violation(s)".formatted(violations.size()));
        this.violations = List.copyOf(violations);
    }
}
