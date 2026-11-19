package com.joshsoftware.templateDrivenDocumentService.engine.contract;

import com.joshsoftware.templateDrivenDocumentService.engine.definition.ContractField;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.DateFormats;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.FieldType;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// One validate call: walks the contract over the mapped JSON, building the model and collecting violations
final class ContractWalker {

    private final DateFormats formats;
    private final List<Violation> violations = new ArrayList<>();

    ContractWalker(DateFormats formats) {
        this.formats = formats;
    }

    List<Violation> violations() {
        return violations;
    }

    // Contract order, so the model reads like the contract; unmodifiable, but null values allowed
    Map<String, Object> readObject(Map<String, ContractField> fields, JsonNode node, String parent) {
        Map<String, Object> model = new LinkedHashMap<>();
        fields.forEach((key, field) -> {
            String path = parent.isEmpty() ? key : parent + "." + key;
            model.put(key, readField(field, node.get(key), path));
        });
        return Collections.unmodifiableMap(model);
    }

    private Object readField(ContractField field, JsonNode value, String path) {
        // Blank text counts as missing for every type, so "" never reaches a parser
        if (value == null || value.isNull() || (value.isString() && value.asString().isBlank())) {
            if (field.required()) {
                violations.add(new Violation(path, ViolationRule.REQUIRED.name()));
            }
            return null;
        }
        // Children are checked only when the parent is present, so one missing object is one violation
        if (field.type() == FieldType.OBJECT) {
            return value.isObject() ? readObject(field.fields(), value, path) : wrongType(path);
        }
        if (field.type() == FieldType.ARRAY) {
            return value.isArray() ? readArray(field.items(), value, path) : wrongType(path);
        }
        if (value.isContainer()) {
            return wrongType(path);
        }
        ScalarReader.Result result = ScalarReader.read(field, value, formats);
        if (result.rule() != null) {
            violations.add(new Violation(path, result.rule().name()));
        }
        return result.value();
    }

    private List<Object> readArray(ContractField items, JsonNode array, String path) {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            list.add(readField(items, array.get(i), path + "[" + i + "]"));
        }
        return Collections.unmodifiableList(list);
    }

    private Object wrongType(String path) {
        violations.add(new Violation(path, ViolationRule.WRONG_TYPE.name()));
        return null;
    }
}
