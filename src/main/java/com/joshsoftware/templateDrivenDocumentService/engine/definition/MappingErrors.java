package com.joshsoftware.templateDrivenDocumentService.engine.definition;

import com.joshsoftware.templateDrivenDocumentService.engine.exception.InvalidDefinitionException;

// Config errors name the mapping field, never a payload value
final class MappingErrors {

    private MappingErrors() {
    }

    static InvalidDefinitionException fail(String field, String detail) {
        return new InvalidDefinitionException("mapping field '%s': %s".formatted(field, detail));
    }

    static InvalidDefinitionException fail(String field, String detail, Throwable cause) {
        return new InvalidDefinitionException("mapping field '%s': %s".formatted(field, detail), cause);
    }

    // "" + "address" -> "address", "address" + "city" -> "address.city"
    static String fieldName(String parent, String key) {
        return parent.isEmpty() ? key : parent + "." + key;
    }
}
