package com.joshsoftware.templateDrivenDocumentService.engine.mapping;

import com.joshsoftware.templateDrivenDocumentService.engine.definition.ContractField;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.DateFormats;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.FieldType;
import tools.jackson.databind.JsonNode;

import java.util.Map;

// Checks the mapping against the contract before a version is saved, at every nesting level
final class MappingConfigChecker {

    private final SystemFunctions system;
    private final DateFormats formats;

    MappingConfigChecker(SystemFunctions system, DateFormats formats) {
        this.system = system;
        this.formats = formats;
    }

    // Both ways: every mapping key is a contract field, every contract field has a mapping entry
    void checkObject(JsonNode map, Map<String, ContractField> fields, String parent) {
        for (String key : map.propertyNames()) {
            if (!fields.containsKey(key)) {
                throw MappingErrors.fail(MappingErrors.fieldName(parent, key), "not in contract");
            }
        }
        fields.forEach((key, field) -> {
            String name = MappingErrors.fieldName(parent, key);
            JsonNode rule = map.get(key);
            if (rule == null) {
                throw MappingErrors.fail(name, "has no mapping (use null if not supplied)");
            }
            checkValue(rule, field, name);
        });
    }

    private void checkValue(JsonNode rule, ContractField field, String name) {
        if (rule.isNull()) {
            return;
        }
        if (rule.isString()) {
            String text = rule.asString();
            if (SystemFunctions.isCall(text)) {
                // A type mismatch would fail every request as the bank's fault (422), so catch it here
                FieldType produces = SystemFunctions.type(text, name);
                if (field.type() != produces) {
                    throw MappingErrors.fail(name, "%s needs a %s field".formatted(text, produces.name().toLowerCase()));
                }
                // Run once now, so a missing or broken date format fails before saving, not on a request
                system.evaluate(text, formats, name);
            } else {
                PayloadPath.parse(text, name);
            }
            return;
        }
        checkForm(ObjectForm.parse(rule, name), field, name);
    }

    // The form's output shape must match the contract type, then its map is checked one level down
    private void checkForm(ObjectForm form, ContractField field, String name) {
        if (form.picksOne()) {
            if (field.type() != FieldType.OBJECT) {
                throw MappingErrors.fail(name, "select needs an object field");
            }
            checkObject(form.map(), field.fields(), name);
            return;
        }
        if (field.type() != FieldType.ARRAY || field.items().type() != FieldType.OBJECT) {
            throw MappingErrors.fail(name, "from/map needs an array of objects field");
        }
        checkObject(form.map(), field.items().fields(), name);
    }
}
