package com.joshsoftware.templateDrivenDocumentService.engine.mapping;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// A checked payload path such as "A.B[0].C"; reading it never fails on missing data
record PayloadPath(List<Segment> segments) {

    private static final Pattern SEGMENT = Pattern.compile("([^.\\[\\]\\s]+)((?:\\[\\d+])*)");
    private static final Pattern INDEX = Pattern.compile("\\[(\\d+)]");

    // "B[1][0]" -> name "B", indexes [1, 0]
    record Segment(String name, List<Integer> indexes) {
    }

    static PayloadPath parse(String path, String field) {
        return parse(path, field, "path");
    }

    // label says which path is broken in the message: "bad path" or "bad from path"
    static PayloadPath parse(String path, String field, String label) {
        List<Segment> segments = new ArrayList<>();
        for (String part : path.split("\\.", -1)) {
            Matcher m = SEGMENT.matcher(part);
            if (!m.matches()) {
                throw MappingErrors.fail(field, "bad " + label);
            }
            segments.add(new Segment(m.group(1), indexes(m.group(2), field, label)));
        }
        return new PayloadPath(List.copyOf(segments));
    }

    // Missing key, out-of-range index or a wrong-shaped node all give JSON null
    JsonNode read(JsonNode root) {
        JsonNode node = root;
        for (Segment segment : segments) {
            node = node != null && node.isObject() ? node.get(segment.name()) : null;
            for (int index : segment.indexes()) {
                node = node == null ? null : at(node, index);
            }
        }
        return node == null ? JsonNodeFactory.instance.nullNode() : node;
    }

    // A single object counts as a one-item list, so [0] reads it
    private static JsonNode at(JsonNode node, int index) {
        if (node.isArray()) {
            return node.get(index);
        }
        return node.isObject() && index == 0 ? node : null;
    }

    private static List<Integer> indexes(String brackets, String field, String label) {
        List<Integer> indexes = new ArrayList<>();
        Matcher m = INDEX.matcher(brackets);
        while (m.find()) {
            try {
                indexes.add(Integer.parseInt(m.group(1)));
            } catch (NumberFormatException e) {
                throw MappingErrors.fail(field, "bad " + label, e);
            }
        }
        return List.copyOf(indexes);
    }
}
