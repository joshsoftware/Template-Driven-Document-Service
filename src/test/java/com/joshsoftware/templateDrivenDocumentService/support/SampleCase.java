package com.joshsoftware.templateDrivenDocumentService.support;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

// One sample from sample-cases.yml: a payload and the file holding the output it must produce
public record SampleCase(String name, String payload, String expected) {

    private static final String FILE = "sample-cases.yml";

    // Cases listed under a stage, such as "mapping"; fails rather than run zero cases
    public static Stream<SampleCase> forStage(String stage) {
        Object section = load().get(stage);
        if (!(section instanceof List<?> entries) || entries.isEmpty()) {
            throw new IllegalStateException("%s: no cases under '%s'".formatted(FILE, stage));
        }
        return entries.stream().map(entry -> toCase(entry, stage));
    }

    // Shown as the test name in reports
    @Override
    public String toString() {
        return name;
    }

    // Safe loader: plain maps and lists only, no Java types from the file
    private static Map<?, ?> load() {
        Object root = new Yaml(new SafeConstructor(new LoaderOptions())).load(Fixtures.text(FILE));
        if (!(root instanceof Map<?, ?> stages)) {
            throw new IllegalStateException(FILE + ": expected stage names at the top level");
        }
        return stages;
    }

    private static SampleCase toCase(Object entry, String stage) {
        if (!(entry instanceof Map<?, ?> fields)) {
            throw new IllegalStateException("%s: '%s' has an entry that is not name/payload/expected".formatted(FILE, stage));
        }
        String name = text(fields, "name", stage);
        return new SampleCase(name, file(fields, "payload", stage, name), file(fields, "expected", stage, name));
    }

    private static String text(Map<?, ?> fields, String key, String stage) {
        if (!(fields.get(key) instanceof String value) || value.isBlank()) {
            throw new IllegalStateException("%s: a '%s' case needs %s".formatted(FILE, stage, key));
        }
        return value;
    }

    // A wrong path fails while listing the cases, naming the case
    private static String file(Map<?, ?> fields, String key, String stage, String name) {
        String path = text(fields, key, stage);
        if (SampleCase.class.getResource(Fixtures.STATEMENT_V1 + path) == null) {
            throw new IllegalStateException("%s: case '%s' %s not found: %s".formatted(FILE, name, key, path));
        }
        return path;
    }
}
