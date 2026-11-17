package com.joshsoftware.templateDrivenDocumentService.engine.contract;

import com.joshsoftware.templateDrivenDocumentService.engine.definition.ContractField;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.DateFormats;
import com.joshsoftware.templateDrivenDocumentService.engine.exception.InvalidDefinitionException;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.BiFunction;

// Converts one present scalar value to its Java type, or names the rule it breaks
final class ScalarReader {

    // Either a converted value or a broken rule, never both
    record Result(Object value, ViolationRule rule) {

        static Result ok(Object value) {
            return new Result(value, null);
        }

        static Result fail(ViolationRule rule) {
            return new Result(null, rule);
        }
    }

    private ScalarReader() {
    }

    static Result read(ContractField field, JsonNode value, DateFormats formats, String path) {
        return switch (field.type()) {
            // A JSON number is rejected, not turned into text: it may already have lost leading zeros
            case STRING -> value.isString() ? Result.ok(value.asString()) : Result.fail(ViolationRule.WRONG_TYPE);
            case NUMBER -> number(value);
            case DATE -> temporal(value, formatter(formats.date(), "DATE", "date", path),
                    LocalDate::parse, ViolationRule.BAD_DATE);
            case DATETIME -> temporal(value, formatter(formats.datetime(), "DATETIME", "datetime", path),
                    LocalDateTime::parse, ViolationRule.BAD_DATETIME);
            case ENUM -> value.isString() && field.values().contains(value.asString())
                    ? Result.ok(value.asString())
                    : Result.fail(ViolationRule.NOT_ALLOWED);
            case OBJECT, ARRAY -> throw new IllegalArgumentException("not a scalar: " + field.type());
        };
    }

    // BigDecimal keeps the exact value and scale, so 14400.00 prints as 14400.00
    private static Result number(JsonNode value) {
        if (value.isNumber()) {
            return Result.ok(value.decimalValue());
        }
        if (!value.isString()) {
            return Result.fail(ViolationRule.NOT_A_NUMBER);
        }
        try {
            return Result.ok(new BigDecimal(value.asString()));
        } catch (NumberFormatException e) {
            return Result.fail(ViolationRule.NOT_A_NUMBER);
        }
    }

    // Formatters are strict, so 20240231 is a broken date, not 29 February
    private static Result temporal(JsonNode value, DateTimeFormatter formatter,
                                   BiFunction<String, DateTimeFormatter, Object> parse, ViolationRule rule) {
        if (!value.isString()) {
            return Result.fail(rule);
        }
        try {
            return Result.ok(parse.apply(value.asString(), formatter));
        } catch (DateTimeParseException e) {
            return Result.fail(rule);
        }
    }

    // A date field without its pattern is a template config bug, not a payload problem
    private static DateTimeFormatter formatter(DateTimeFormatter formatter, String type, String name, String path) {
        if (formatter == null) {
            throw new InvalidDefinitionException(
                    "contract field '%s': %s needs dateFormats.%s".formatted(path, type, name));
        }
        return formatter;
    }
}
