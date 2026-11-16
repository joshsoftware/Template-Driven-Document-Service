package com.joshsoftware.templateDrivenDocumentService.engine.definition;

import com.joshsoftware.templateDrivenDocumentService.engine.exception.InvalidDefinitionException;

import java.util.Locale;

// Kinds of value a contract field can hold
public enum FieldType {

    STRING, NUMBER, DATE, DATETIME, ENUM, ARRAY, OBJECT;

    // Reads the contract's lowercase type name, e.g. "string"
    public static FieldType fromJson(String name, String fieldPath) {
        if (name == null || name.isBlank()) {
            throw new InvalidDefinitionException("contract field '%s': type is required".formatted(fieldPath));
        }
        for (FieldType type : values()) {
            if (type.name().toLowerCase(Locale.ROOT).equals(name)) {
                return type;
            }
        }
        throw new InvalidDefinitionException("contract field '%s': unknown type".formatted(fieldPath));
    }

    public boolean isScalar() {
        return this != ARRAY && this != OBJECT;
    }
}
