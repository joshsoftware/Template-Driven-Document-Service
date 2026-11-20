package com.joshsoftware.templateDrivenDocumentService.engine.definition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

// One compiled mapping.json rule; checked and built once at load, immutable after
public sealed interface MappingRule {

    // null in mapping.json: the field is always null
    record NullValue() implements MappingRule {
    }

    // "A.B[0].C"
    record Path(PayloadPath path) implements MappingRule {

        public Path {
            Objects.requireNonNull(path, "path");
        }
    }

    // system.today() or system.now()
    record SystemCall(SystemFunction function) implements MappingRule {

        public SystemCall {
            Objects.requireNonNull(function, "function");
        }
    }

    // {from, map}: one row per item
    record ListOf(PayloadPath from, Map<String, MappingRule> map) implements MappingRule {

        public ListOf {
            Objects.requireNonNull(from, "from");
            map = frozen(map);
        }
    }

    // {from, select, map}: one object, the first item matching select
    record PickOne(PayloadPath from, Map<String, String> select, Map<String, MappingRule> map) implements MappingRule {

        public PickOne {
            Objects.requireNonNull(from, "from");
            select = frozen(select);
            map = frozen(map);
        }
    }

    enum SystemFunction { TODAY, NOW }

    // Keeps mapping order, so output keys follow the file
    private static <V> Map<String, V> frozen(Map<String, V> map) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(map, "map")));
    }
}
