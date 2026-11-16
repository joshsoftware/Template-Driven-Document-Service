package com.joshsoftware.templateDrivenDocumentService.engine.definition;

import com.joshsoftware.templateDrivenDocumentService.engine.exception.InvalidDefinitionException;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;

// How the bank writes dates (mapping.json "dateFormats"); either pattern may be absent
public record DateFormats(
        String datePattern,
        String datetimePattern,
        DateTimeFormatter date,
        DateTimeFormatter datetime) {

    public static final DateFormats NONE = new DateFormats(null, null, null, null);

    public static DateFormats of(String datePattern, String datetimePattern) {
        return new DateFormats(datePattern, datetimePattern,
                compile(datePattern, "date"), compile(datetimePattern, "datetime"));
    }

    // Strict, so 20240231 is rejected instead of rolling over to March
    private static DateTimeFormatter compile(String pattern, String name) {
        if (pattern == null) {
            return null;
        }
        if (pattern.isBlank()) {
            throw new InvalidDefinitionException("mapping: dateFormats.%s is blank".formatted(name));
        }
        try {
            // Bank dates carry no era, and strict mode can't resolve 'yyyy' without one, so assume AD
            return new DateTimeFormatterBuilder()
                    .appendPattern(pattern)
                    .parseDefaulting(ChronoField.ERA, 1)
                    .toFormatter()
                    .withResolverStyle(ResolverStyle.STRICT);
        } catch (IllegalArgumentException e) {
            throw new InvalidDefinitionException("mapping: dateFormats.%s is not a valid pattern".formatted(name), e);
        }
    }
}
