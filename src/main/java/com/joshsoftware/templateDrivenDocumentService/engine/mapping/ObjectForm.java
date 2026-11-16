package com.joshsoftware.templateDrivenDocumentService.engine.mapping;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// The object forms: list {from, map} and pick-one {from, select, map}
record ObjectForm(PayloadPath from, JsonNode select, JsonNode map) {

    private static final Set<String> KEYS = Set.of("from", "select", "map");

    static ObjectForm parse(JsonNode rule, String field) {
        if (!rule.isObject()) {
            throw MappingErrors.fail(field, "must be a path, system function, object or null");
        }
        for (String key : rule.propertyNames()) {
            if (!KEYS.contains(key)) {
                throw MappingErrors.fail(field, "unknown key '%s'".formatted(key));
            }
        }
        JsonNode from = rule.get("from");
        if (from == null || !from.isString()) {
            throw MappingErrors.fail(field, "needs from");
        }
        JsonNode map = rule.get("map");
        if (map == null || !map.isObject() || map.isEmpty()) {
            throw MappingErrors.fail(field, "needs map");
        }
        JsonNode select = rule.get("select");
        if (select != null && (!select.isObject() || !select.valueStream().allMatch(JsonNode::isString))) {
            throw MappingErrors.fail(field, "select must be an object of strings");
        }
        return new ObjectForm(PayloadPath.parse(from.asString(), field, "from path"), select, map);
    }

    boolean picksOne() {
        return select != null;
    }

    // Source is an array or an object; a single object counts as a one-item list
    static List<JsonNode> items(JsonNode source) {
        if (!source.isArray()) {
            return List.of(source);
        }
        List<JsonNode> items = new ArrayList<>();
        source.forEach(items::add);
        return items;
    }

    // First item matching select, else the first item, else empty
    Optional<JsonNode> pick(List<JsonNode> items) {
        return items.stream().filter(this::matches).findFirst().or(() -> items.stream().findFirst());
    }

    // Every select key must equal the item's value as text; select {} matches any item
    private boolean matches(JsonNode item) {
        for (Map.Entry<String, JsonNode> entry : select.properties()) {
            JsonNode value = item.get(entry.getKey());
            if (value == null || !value.isValueNode() || value.isNull()
                    || !value.asString().equals(entry.getValue().asString())) {
                return false;
            }
        }
        return true;
    }
}
