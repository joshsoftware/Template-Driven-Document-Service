package com.joshsoftware.templateDrivenDocumentService.engine.definition;

import com.joshsoftware.templateDrivenDocumentService.engine.exception.InvalidDefinitionException;
import tools.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// One template version, ready to run: template, contract and mapping versioned together
public record DocumentDefinition(
        UUID templateVersionId,
        String templateId,
        int version,
        String templateHtml,
        Map<String, ContractField> contract,
        DateFormats dateFormats,
        JsonNode transform) {

    public DocumentDefinition {
        Objects.requireNonNull(templateVersionId, "templateVersionId");
        Objects.requireNonNull(templateId, "templateId");
        Objects.requireNonNull(templateHtml, "templateHtml");
        Objects.requireNonNull(contract, "contract");
        Objects.requireNonNull(dateFormats, "dateFormats");
        Objects.requireNonNull(transform, "transform");
        if (templateId.isBlank()) {
            throw new InvalidDefinitionException("templateId is blank");
        }
        if (version < 1) {
            throw new InvalidDefinitionException("version must be 1 or more");
        }
        if (contract.isEmpty()) {
            throw new InvalidDefinitionException("contract: needs fields");
        }
        if (!transform.isObject()) {
            throw new InvalidDefinitionException("mapping: transform must be an object");
        }
        contract = Collections.unmodifiableMap(new LinkedHashMap<>(contract));
        // Own copy, so nothing outside can change the mapping after load
        transform = transform.deepCopy();
    }
}
