package com.joshsoftware.templateDrivenDocumentService.annotation;

import com.joshsoftware.templateDrivenDocumentService.util.validator.NonEmptyJsonObjectValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// The annotated JsonNode must be a JSON object with at least one field.
// Missing, null, {}, arrays, strings and numbers all fail.
@Documented
@Constraint(validatedBy = NonEmptyJsonObjectValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NonEmptyJsonObject {

    String message() default "must be a non-empty JSON object";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
