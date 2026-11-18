package com.joshsoftware.templateDrivenDocumentService.util.validator;

import com.joshsoftware.templateDrivenDocumentService.annotation.NonEmptyJsonObject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import tools.jackson.databind.JsonNode;

// Backs @NonEmptyJsonObject. Never logs or echoes the node: it may hold customer data.
public class NonEmptyJsonObjectValidator implements ConstraintValidator<NonEmptyJsonObject, JsonNode> {

    @Override
    public boolean isValid(JsonNode value, ConstraintValidatorContext context) {
        return value != null && value.isObject() && !value.isEmpty();
    }
}
