package com.joshsoftware.templateDrivenDocumentService.engine.definition;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// contract.json -> ordered ContractField tree, recursing through fields (object) and items (array)
final class ContractReader {

    private ContractReader() {
    }

    static Map<String, ContractField> read(JsonNode contract) {
        return readFields(contract.path("fields"), "");
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
        JsonNode type = node.get("type");
        return new ContractField(
                FieldType.fromJson(type == null ? null : type.asString(), path),
                // Absent means optional
                node.path("required").asBoolean(false),
                node.has("fields") ? readFields(node.get("fields"), path) : null,
                node.has("items") ? readField(node.get("items"), path + "[]") : null,
                node.has("values") ? strings(node.get("values")) : null);
    }

    private static List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.asString()));
        return values;
    }
}
