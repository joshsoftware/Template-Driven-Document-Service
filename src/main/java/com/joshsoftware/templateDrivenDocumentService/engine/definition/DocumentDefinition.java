package com.joshsoftware.templateDrivenDocumentService.engine.definition;

import com.joshsoftware.templateDrivenDocumentService.engine.exception.InvalidDefinitionException;
import tools.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// One template version, ready to run: template, contract and compiled mapping versioned together
public record DocumentDefinition(
        UUID templateVersionId,
        String templateId,
        int version,
        String templateHtml,
        Map<String, ContractField> contract,
        DateFormats dateFormats,
        Map<String, MappingRule> mapping) {

    public DocumentDefinition {
        Objects.requireNonNull(templateVersionId, "templateVersionId");
        Objects.requireNonNull(templateId, "templateId");
        Objects.requireNonNull(templateHtml, "templateHtml");
        Objects.requireNonNull(contract, "contract");
        Objects.requireNonNull(dateFormats, "dateFormats");
        Objects.requireNonNull(mapping, "mapping");
        if (templateId.isBlank()) {
            throw new InvalidDefinitionException("templateId is blank");
        }
        if (version < 1) {
            throw new InvalidDefinitionException("version must be 1 or more");
        }
        if (contract.isEmpty()) {
            throw new InvalidDefinitionException("contract: needs fields");
        }
        contract = Collections.unmodifiableMap(new LinkedHashMap<>(contract));
        mapping = Collections.unmodifiableMap(new LinkedHashMap<>(mapping));
        MappingReader.checkDates(mapping, dateFormats, "");
    }

    // Compiles mapping.json's transform, so a broken rule fails at load, never on a request
    public DocumentDefinition(UUID templateVersionId, String templateId, int version, String templateHtml,
                              Map<String, ContractField> contract, DateFormats dateFormats, JsonNode transform) {
        this(templateVersionId, templateId, version, templateHtml, contract, dateFormats,
                MappingReader.read(transform));
    }
}
