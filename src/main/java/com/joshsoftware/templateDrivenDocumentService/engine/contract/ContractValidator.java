package com.joshsoftware.templateDrivenDocumentService.engine.contract;

import com.joshsoftware.templateDrivenDocumentService.engine.definition.DocumentDefinition;
import tools.jackson.databind.JsonNode;

import java.util.Map;

// Checks mapped JSON against the contract and converts it to the typed model the template reads
public interface ContractValidator {

    // One pass: returns the model, or throws DataContractViolationException listing every failed field
    Map<String, Object> validate(DocumentDefinition def, JsonNode mapped);
}
