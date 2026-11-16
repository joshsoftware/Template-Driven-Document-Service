package com.joshsoftware.templateDrivenDocumentService.engine.definition;

import com.joshsoftware.templateDrivenDocumentService.engine.exception.InvalidDefinitionException;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Rule for one contract field; recursive through fields (OBJECT) and items (ARRAY)
public record ContractField(
        FieldType type,
        boolean required,
        Map<String, ContractField> fields,
        ContractField items,
        List<String> values) {

    public ContractField {
        Objects.requireNonNull(type, "type");
        fields = fields == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(fields));
        values = values == null ? List.of() : List.copyOf(values);

        switch (type) {
            case OBJECT -> {
                require(!fields.isEmpty(), "OBJECT needs fields");
                require(items == null && values.isEmpty(), "OBJECT allows only fields");
            }
            case ARRAY -> {
                require(items != null, "ARRAY needs items");
                require(fields.isEmpty() && values.isEmpty(), "ARRAY allows only items");
            }
            case ENUM -> {
                require(!values.isEmpty(), "ENUM needs values");
                require(new HashSet<>(values).size() == values.size(), "ENUM values must be distinct");
                require(fields.isEmpty() && items == null, "ENUM allows only values");
            }
            default -> require(fields.isEmpty() && items == null && values.isEmpty(),
                    type + " allows no fields, items or values");
        }
    }

    public static ContractField scalar(FieldType type, boolean required) {
        return new ContractField(type, required, null, null, null);
    }

    public static ContractField object(boolean required, Map<String, ContractField> fields) {
        return new ContractField(FieldType.OBJECT, required, fields, null, null);
    }

    public static ContractField array(boolean required, ContractField items) {
        return new ContractField(FieldType.ARRAY, required, null, items, null);
    }

    public static ContractField enumOf(boolean required, List<String> values) {
        return new ContractField(FieldType.ENUM, required, null, null, values);
    }

    private static void require(boolean condition, String detail) {
        if (!condition) {
            throw new InvalidDefinitionException("contract field: " + detail);
        }
    }
}
