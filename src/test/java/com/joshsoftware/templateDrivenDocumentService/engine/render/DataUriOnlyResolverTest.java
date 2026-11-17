package com.joshsoftware.templateDrivenDocumentService.engine.render;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataUriOnlyResolverTest {

    private final DataUriOnlyResolver resolver = new DataUriOnlyResolver();

    @Test
    void passesDataUrisThroughUnchanged() {
        String uri = "data:image/png;base64,iVBORw0KGgo=";

        assertThat(resolver.resolveURI(null, uri)).isEqualTo(uri);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"http://example.com/a.png", "https://example.com/a.png", "file:///etc/passwd",
            "ftp://example.com/a.png", "jar:file:/app.jar!/a.png", "a.png", "../a.png", "DATA:image/png;base64,AA"})
    void refusesEverythingElse(String uri) {
        assertThatThrownBy(() -> resolver.resolveURI(null, uri))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("only data: URIs may be loaded");
    }
}
