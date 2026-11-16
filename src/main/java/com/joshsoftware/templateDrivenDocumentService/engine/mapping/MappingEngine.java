package com.joshsoftware.templateDrivenDocumentService.engine.mapping;

import com.joshsoftware.templateDrivenDocumentService.engine.definition.DocumentDefinition;
import tools.jackson.databind.JsonNode;

// Turns the bank payload into JSON shaped like the contract, using the definition's mapping
public interface MappingEngine {

    // Before a version is saved (the parser calls it): checks the mapping against the contract
    void checkConfig(DocumentDefinition def);

    // Per request: missing payload data becomes null, the validator decides if that is allowed
    JsonNode transform(DocumentDefinition def, JsonNode payload);
}
