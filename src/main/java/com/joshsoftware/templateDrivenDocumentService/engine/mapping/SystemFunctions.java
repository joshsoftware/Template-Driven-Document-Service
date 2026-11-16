package com.joshsoftware.templateDrivenDocumentService.engine.mapping;

import com.joshsoftware.templateDrivenDocumentService.engine.definition.DateFormats;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// system.today() and system.now(), printed in the bank's date format
final class SystemFunctions {

    private static final String PREFIX = "system.";
    private static final Pattern CALL = Pattern.compile("system\\.(\\w+)\\(\\)");

    private final LocalDateTime now;

    private SystemFunctions(LocalDateTime now) {
        this.now = now;
    }

    // One clock reading per document, so statementDate and generatedAt agree even across midnight
    static SystemFunctions readAt(Clock clock) {
        return new SystemFunctions(LocalDateTime.now(clock));
    }

    static boolean isCall(String text) {
        return text.startsWith(PREFIX);
    }

    String evaluate(String text, DateFormats formats, String field) {
        Matcher m = CALL.matcher(text);
        if (!m.matches()) {
            throw MappingErrors.fail(field, "bad system function");
        }
        try {
            return switch (m.group(1)) {
                case "today" -> now.toLocalDate().format(required(formats.date(), "date", field));
                case "now" -> now.format(required(formats.datetime(), "datetime", field));
                default -> throw MappingErrors.fail(field, "unknown system function");
            };
        } catch (DateTimeException e) {
            throw MappingErrors.fail(field, "date format cannot print this value", e);
        }
    }

    // dateFormats is needed only by templates that call system.*
    private static DateTimeFormatter required(DateTimeFormatter formatter, String name, String field) {
        if (formatter == null) {
            throw MappingErrors.fail(field, "dateFormats.%s is required".formatted(name));
        }
        return formatter;
    }
}
