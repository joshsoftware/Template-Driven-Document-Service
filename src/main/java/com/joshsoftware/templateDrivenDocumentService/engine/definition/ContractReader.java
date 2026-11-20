package com.joshsoftware.templateDrivenDocumentService.engine.definition;

import com.joshsoftware.templateDrivenDocumentService.engine.exception.InvalidDefinitionException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// contract.json -> ordered ContractField tree; strict, so a typo fails instead of changing the contract
final class ContractReader {

    private static final Set<String> TOP_KEYS = Set.of("fields");
    private static final Set<String> FIELD_KEYS = Set.of("type", "required", "fields", "items", "values");

    private ContractReader() {
    }

    static Map<String, ContractField> read(JsonNode contract) {
        for (String key : contract.propertyNames()) {
            if (!TOP_KEYS.contains(key)) {
                throw new InvalidDefinitionException("contract: unknown key '%s'".formatted(key));
            }
        }
        JsonNode fields = contract.get("fields");
        if (fields == null || !fields.isObject()) {
            throw new InvalidDefinitionException("contract: fields must be an object");
        }
        return readFields(fields, "");
    }

    // Keeps file order, so the model follows the contract
    private static Map<String, ContractField> readFields(JsonNode fields, String parent) {
        Map<String, ContractField> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> entry : fields.properties()) {
            String path = parent.isEmpty() ? entry.getKey() : parent + "." + entry.getKey();
            result.put(entry.getKey(), readField(entry.getValue(), path));
        }
        return result;
    }

    private static ContractField readField(JsonNode node, String path) {
        if (!node.isObject()) {
            throw fail(path, "must be an object");
        }
        for (String key : node.propertyNames()) {
            if (!FIELD_KEYS.contains(key)) {
                throw fail(path, "unknown key '%s'".formatted(key));
            }
        }
        JsonNode type = node.get("type");
        FieldType fieldType = FieldType.fromJson(type == null ? null : type.asString(), path);
        Map<String, ContractField> fields = node.has("fields") ? readFields(objectAt(node, "fields", path), path) : null;
        ContractField items = node.has("items") ? readField(node.get("items"), path + "[]") : null;
        List<String> values = node.has("values") ? strings(node.get("values"), path) : null;

        // Checked here too, so the error names the field
        String problem = ContractField.problem(fieldType, fields, items, values);
        if (problem != null) {
            throw fail(path, problem);
        }
        return new ContractField(fieldType, required(node, path), fields, items, values);
    }

    // Absent means optional; anything but true/false is a mistake, not false
    private static boolean required(JsonNode node, String path) {
        JsonNode required = node.get("required");
        if (required == null) {
            return false;
        }
        if (!required.isBoolean()) {
            throw fail(path, "required must be true or false");
        }
        return required.booleanValue();
    }

    private static JsonNode objectAt(JsonNode node, String key, String path) {
        JsonNode value = node.get(key);
        if (!value.isObject()) {
            throw fail(path, key + " must be an object");
        }
        return value;
    }

    private static List<String> strings(JsonNode array, String path) {
        if (!array.isArray()) {
            throw fail(path, "values must be an array of strings");
        }
        List<String> values = new ArrayList<>();
        for (JsonNode value : array) {
            if (!value.isString()) {
                throw fail(path, "values must be an array of strings");
            }
            values.add(value.asString());
        }
        return values;
    }

    private static InvalidDefinitionException fail(String path, String detail) {
        return new InvalidDefinitionException("contract field '%s': %s".formatted(path, detail));
    }
}
