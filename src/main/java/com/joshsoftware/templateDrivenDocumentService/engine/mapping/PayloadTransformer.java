package com.joshsoftware.templateDrivenDocumentService.engine.mapping;

import com.joshsoftware.templateDrivenDocumentService.engine.definition.DateFormats;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

// One transform call: walks the mapping and builds contract-shaped JSON from the payload
final class PayloadTransformer {

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private final SystemFunctions system;
    private final DateFormats formats;

    PayloadTransformer(SystemFunctions system, DateFormats formats) {
        this.system = system;
        this.formats = formats;
    }

    // map says which keys to build, context is the payload part its paths read from
    ObjectNode mapObject(JsonNode map, JsonNode context, String parent) {
        ObjectNode out = NODES.objectNode();
        for (Map.Entry<String, JsonNode> entry : map.properties()) {
            String field = MappingErrors.fieldName(parent, entry.getKey());
            out.set(entry.getKey(), mapValue(entry.getValue(), context, field));
        }
        return out;
    }

    private JsonNode mapValue(JsonNode rule, JsonNode context, String field) {
        if (rule.isNull()) {
            return NODES.nullNode();
        }
        if (rule.isString()) {
            String text = rule.asString();
            return SystemFunctions.isCall(text)
                    ? NODES.stringNode(system.evaluate(text, formats, field))
                    : PayloadPath.parse(text, field).read(context);
        }
        return mapForm(ObjectForm.parse(rule, field), context, field);
    }

    // The list form gives one row per item, the pick-one form one object
    private JsonNode mapForm(ObjectForm form, JsonNode context, String field) {
        JsonNode source = form.from().read(context);
        // Missing, or a scalar such as "" for "no transactions", is not a row: give null, not a row of nulls
        if (!source.isArray() && !source.isObject()) {
            return NODES.nullNode();
        }
        List<JsonNode> items = ObjectForm.items(source);
        if (form.picksOne()) {
            return form.pick(items)
                    .<JsonNode>map(item -> mapObject(form.map(), item, field))
                    .orElse(NODES.nullNode());
        }
        ArrayNode rows = NODES.arrayNode();
        items.forEach(item -> rows.add(mapObject(form.map(), item, field)));
        return rows;
    }
}
