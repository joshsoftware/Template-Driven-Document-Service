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

        String problem = problem(type, fields, items, values);
        if (problem != null) {
            throw new InvalidDefinitionException("contract field: " + problem);
        }
    }

    // First shape rule broken, or null; shared so ContractReader can name the field's path
    static String problem(FieldType type, Map<String, ContractField> fields, ContractField items, List<String> values) {
        boolean noFields = fields == null || fields.isEmpty();
        boolean noValues = values == null || values.isEmpty();
        return switch (type) {
            case OBJECT -> noFields ? "OBJECT needs fields"
                    : items != null || !noValues ? "OBJECT allows only fields" : null;
            case ARRAY -> items == null ? "ARRAY needs items"
                    : !noFields || !noValues ? "ARRAY allows only items" : null;
            case ENUM -> noValues ? "ENUM needs values"
                    : new HashSet<>(values).size() != values.size() ? "ENUM values must be distinct"
                    : !noFields || items != null ? "ENUM allows only values" : null;
            default -> !noFields || items != null || !noValues ? type + " allows no fields, items or values" : null;
        };
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
}
