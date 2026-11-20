package com.joshsoftware.templateDrivenDocumentService.engine.definition;

import com.joshsoftware.templateDrivenDocumentService.engine.exception.InvalidDefinitionException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

// contract.json + mapping.json -> DocumentDefinition; any broken config fails here as InvalidDefinitionException
public final class DocumentDefinitionParser {

    private static final Set<String> MAPPING_KEYS = Set.of("dateFormats", "transform");
    private static final Set<String> DATE_FORMAT_KEYS = Set.of("date", "datetime");

    private final ObjectReader reader;

    public DocumentDefinitionParser(JsonMapper json) {
        // Own reader, so the shared mapper is untouched; a duplicate key would silently override the first
        this.reader = Objects.requireNonNull(json, "json").reader().with(StreamReadFeature.STRICT_DUPLICATE_DETECTION);
    }

    public DocumentDefinition parse(UUID templateVersionId, String templateId, int version,
                                    String templateHtml, String contractJson, String mappingJson) {
        JsonNode mapping = readObject(mappingJson, "mapping");
        checkKeys(mapping, MAPPING_KEYS, "mapping");
        JsonNode transform = mapping.get("transform");
        if (transform == null) {
            throw new InvalidDefinitionException("mapping: transform is required");
        }
        return new DocumentDefinition(templateVersionId, templateId, version, templateHtml,
                ContractReader.read(readObject(contractJson, "contract")),
                dateFormats(mapping.get("dateFormats")),
                transform);
    }

    // Bad JSON is a config bug: report it as one, naming the file
    private JsonNode readObject(String text, String file) {
        if (text == null || text.isBlank()) {
            throw new InvalidDefinitionException(file + ": is empty");
        }
        JsonNode node;
        try {
            node = reader.readTree(text);
        } catch (JacksonException e) {
            throw new InvalidDefinitionException(file + ": is not valid JSON", e);
        }
        if (!node.isObject()) {
            throw new InvalidDefinitionException(file + ": must be a JSON object");
        }
        return node;
    }

    // Optional: a template with no dates needs no dateFormats
    private static DateFormats dateFormats(JsonNode node) {
        if (node == null) {
            return DateFormats.NONE;
        }
        if (!node.isObject()) {
            throw new InvalidDefinitionException("mapping: dateFormats must be an object");
        }
        checkKeys(node, DATE_FORMAT_KEYS, "mapping: dateFormats");
        return DateFormats.of(pattern(node, "date"), pattern(node, "datetime"));
    }

    // A number like 8 is a valid literal pattern and would print "8" as the date, so require text
    private static String pattern(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null) {
            return null;
        }
        if (!value.isString()) {
            throw new InvalidDefinitionException("mapping: dateFormats.%s must be a pattern string".formatted(name));
        }
        return value.asString();
    }

    private static void checkKeys(JsonNode node, Set<String> allowed, String where) {
        for (String key : node.propertyNames()) {
            if (!allowed.contains(key)) {
                throw new InvalidDefinitionException("%s: unknown key '%s'".formatted(where, key));
            }
        }
    }
}
