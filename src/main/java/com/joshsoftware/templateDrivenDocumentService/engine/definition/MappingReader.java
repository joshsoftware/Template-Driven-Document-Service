package com.joshsoftware.templateDrivenDocumentService.engine.definition;

import com.joshsoftware.templateDrivenDocumentService.engine.definition.MappingRule.ListOf;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.MappingRule.NullValue;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.MappingRule.Path;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.MappingRule.PickOne;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.MappingRule.SystemCall;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.MappingRule.SystemFunction;
import com.joshsoftware.templateDrivenDocumentService.engine.exception.InvalidDefinitionException;
import tools.jackson.databind.JsonNode;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.joshsoftware.templateDrivenDocumentService.engine.definition.MappingErrors.fail;
import static com.joshsoftware.templateDrivenDocumentService.engine.definition.MappingErrors.fieldName;

// mapping.json "transform" -> MappingRule tree; checks every rule, whatever a payload would reach
final class MappingReader {

    private static final String SYSTEM_PREFIX = "system.";
    private static final Pattern SYSTEM_CALL = Pattern.compile("system\\.(\\w+)\\(\\)");
    private static final Set<String> FORM_KEYS = Set.of("from", "select", "map");
    private static final LocalDateTime SAMPLE = LocalDateTime.of(2000, 1, 1, 0, 0);

    private MappingReader() {
    }

    static Map<String, MappingRule> read(JsonNode transform) {
        Objects.requireNonNull(transform, "transform");
        if (!transform.isObject()) {
            throw new InvalidDefinitionException("mapping: transform must be an object");
        }
        return readMap(transform, "");
    }

    // Every system.* call needs its date format, and the format must be able to print it
    static void checkDates(Map<String, MappingRule> rules, DateFormats formats, String parent) {
        rules.forEach((key, rule) -> {
            String field = fieldName(parent, key);
            switch (rule) {
                case SystemCall call -> checkDate(call.function(), formats, field);
                case ListOf list -> checkDates(list.map(), formats, field);
                case PickOne pick -> checkDates(pick.map(), formats, field);
                case Path path -> {
                }
                case NullValue none -> {
                }
            }
        });
    }

    private static Map<String, MappingRule> readMap(JsonNode map, String parent) {
        Map<String, MappingRule> rules = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> entry : map.properties()) {
            rules.put(entry.getKey(), readRule(entry.getValue(), fieldName(parent, entry.getKey())));
        }
        return rules;
    }

    private static MappingRule readRule(JsonNode rule, String field) {
        if (rule.isNull()) {
            return new NullValue();
        }
        if (rule.isString()) {
            String text = rule.asString();
            return text.startsWith(SYSTEM_PREFIX)
                    ? new SystemCall(systemFunction(text, field))
                    : new Path(PayloadPath.parse(text, field, "path"));
        }
        return readForm(rule, field);
    }

    private static SystemFunction systemFunction(String text, String field) {
        Matcher m = SYSTEM_CALL.matcher(text);
        if (!m.matches()) {
            throw fail(field, "bad system function");
        }
        return switch (m.group(1)) {
            case "today" -> SystemFunction.TODAY;
            case "now" -> SystemFunction.NOW;
            default -> throw fail(field, "unknown system function");
        };
    }

    // List {from, map} or pick-one {from, select, map}; the nested map is read now, not when rows arrive
    private static MappingRule readForm(JsonNode rule, String field) {
        if (!rule.isObject()) {
            throw fail(field, "must be a path, system function, object or null");
        }
        for (String key : rule.propertyNames()) {
            if (!FORM_KEYS.contains(key)) {
                throw fail(field, "unknown key '%s'".formatted(key));
            }
        }
        JsonNode from = rule.get("from");
        if (from == null || !from.isString()) {
            throw fail(field, "needs from");
        }
        JsonNode map = rule.get("map");
        if (map == null || !map.isObject() || map.isEmpty()) {
            throw fail(field, "needs map");
        }
        JsonNode select = rule.get("select");
        if (select != null && (!select.isObject() || !select.valueStream().allMatch(JsonNode::isString))) {
            throw fail(field, "select must be an object of strings");
        }
        PayloadPath fromPath = PayloadPath.parse(from.asString(), field, "from path");
        Map<String, MappingRule> rules = readMap(map, field);
        return select == null ? new ListOf(fromPath, rules) : new PickOne(fromPath, strings(select), rules);
    }

    private static Map<String, String> strings(JsonNode select) {
        Map<String, String> values = new LinkedHashMap<>();
        select.properties().forEach(entry -> values.put(entry.getKey(), entry.getValue().asString()));
        return values;
    }

    private static void checkDate(SystemFunction function, DateFormats formats, String field) {
        switch (function) {
            case TODAY -> checkPrints(formats.date(), "date", SAMPLE.toLocalDate(), field);
            case NOW -> checkPrints(formats.datetime(), "datetime", SAMPLE, field);
        }
    }

    // A pattern like "yyyyMMdd HH" compiles but cannot print a date, so try it once here
    private static void checkPrints(DateTimeFormatter formatter, String name, TemporalAccessor sample, String field) {
        if (formatter == null) {
            throw fail(field, "dateFormats.%s is required".formatted(name));
        }
        try {
            formatter.format(sample);
        } catch (DateTimeException e) {
            throw fail(field, "dateFormats.%s cannot print this value".formatted(name), e);
        }
    }
}
