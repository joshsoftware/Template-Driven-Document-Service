package com.joshsoftware.templateDrivenDocumentService.engine.mapping;

import com.joshsoftware.templateDrivenDocumentService.engine.definition.DateFormats;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.MappingRule;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.MappingRule.ListOf;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.MappingRule.NullValue;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.MappingRule.Path;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.MappingRule.PickOne;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.MappingRule.SystemCall;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.MappingRule.SystemFunction;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// One transform call: walks the compiled mapping and builds contract-shaped JSON from the payload
final class PayloadTransformer {

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private final LocalDateTime now;
    private final DateFormats formats;

    PayloadTransformer(LocalDateTime now, DateFormats formats) {
        this.now = now;
        this.formats = formats;
    }

    // rules say which keys to build, context is the payload part their paths read from
    ObjectNode mapObject(Map<String, MappingRule> rules, JsonNode context) {
        ObjectNode out = NODES.objectNode();
        rules.forEach((key, rule) -> out.set(key, apply(rule, context)));
        return out;
    }

    private JsonNode apply(MappingRule rule, JsonNode context) {
        return switch (rule) {
            case NullValue none -> NODES.nullNode();
            case Path path -> path.path().read(context);
            case SystemCall call -> NODES.stringNode(print(call.function()));
            case ListOf list -> mapList(list, context);
            case PickOne pick -> mapPickOne(pick, context);
        };
    }

    // Formats were checked at load, so printing cannot fail here
    private String print(SystemFunction function) {
        return switch (function) {
            case TODAY -> now.toLocalDate().format(formats.date());
            case NOW -> now.format(formats.datetime());
        };
    }

    private JsonNode mapList(ListOf list, JsonNode context) {
        JsonNode source = list.from().read(context);
        // Missing, or a scalar such as "" for "no transactions", is not a row: give null, not a row of nulls
        if (!source.isArray() && !source.isObject()) {
            return NODES.nullNode();
        }
        ArrayNode rows = NODES.arrayNode();
        items(source).forEach(item -> rows.add(mapObject(list.map(), item)));
        return rows;
    }

    // First item matching select, else the first item, else null
    private JsonNode mapPickOne(PickOne pick, JsonNode context) {
        JsonNode source = pick.from().read(context);
        if (!source.isArray() && !source.isObject()) {
            return NODES.nullNode();
        }
        List<JsonNode> items = items(source);
        return items.stream().filter(item -> matches(pick.select(), item)).findFirst()
                .or(() -> items.stream().findFirst())
                .<JsonNode>map(item -> mapObject(pick.map(), item))
                .orElse(NODES.nullNode());
    }

    // Source is an array or an object; a single object counts as a one-item list
    private static List<JsonNode> items(JsonNode source) {
        if (!source.isArray()) {
            return List.of(source);
        }
        List<JsonNode> items = new ArrayList<>();
        source.forEach(items::add);
        return items;
    }

    // Every select key must equal the item's value as text; select {} matches any item
    private static boolean matches(Map<String, String> select, JsonNode item) {
        for (Map.Entry<String, String> entry : select.entrySet()) {
            JsonNode value = item.get(entry.getKey());
            if (value == null || !value.isValueNode() || value.isNull() || !value.asString().equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }
}
