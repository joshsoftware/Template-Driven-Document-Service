package com.joshsoftware.templateDrivenDocumentService.support;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

// Loads test files from src/test/resources/statement/v1
public final class Fixtures {

    public static final String STATEMENT_V1 = "/statement/v1/";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private Fixtures() {
    }

    public static byte[] bytes(String path) {
        try (InputStream in = Fixtures.class.getResourceAsStream(STATEMENT_V1 + path)) {
            if (in == null) {
                throw new IllegalArgumentException("Fixture not found: " + path);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String text(String path) {
        return new String(bytes(path), StandardCharsets.UTF_8);
    }

    public static JsonNode json(String path) {
        return JSON.readTree(text(path));
    }
}
