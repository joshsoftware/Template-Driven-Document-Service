package com.joshsoftware.templateDrivenDocumentService.engine.definition;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Objects;
import java.util.UUID;

// Template config strings (contract.json, mapping.json) -> the DocumentDefinition objects the engine works on
public final class DocumentDefinitionParser {

    private final JsonMapper json;

    public DocumentDefinitionParser(JsonMapper json) {
        this.json = Objects.requireNonNull(json, "json");
    }

    public DocumentDefinition parse(UUID templateVersionId, String templateId, int version,
                                    String templateHtml, String contractJson, String mappingJson) {
        JsonNode mapping = json.readTree(mappingJson);
        return new DocumentDefinition(templateVersionId, templateId, version, templateHtml,
                ContractReader.read(json.readTree(contractJson)),
                dateFormats(mapping.path("dateFormats")),
                mapping.get("transform"));
    }

    // Optional: a template with no dates needs no dateFormats
    private static DateFormats dateFormats(JsonNode node) {
        if (!node.isObject()) {
            return DateFormats.NONE;
        }
        return DateFormats.of(text(node, "date"), text(node, "datetime"));
    }

    private static String text(JsonNode node, String name) {
        JsonNode value = node.get(name);
        return value == null ? null : value.asString();
    }
}
