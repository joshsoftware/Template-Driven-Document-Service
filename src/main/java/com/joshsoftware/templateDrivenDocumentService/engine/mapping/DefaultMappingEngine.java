package com.joshsoftware.templateDrivenDocumentService.engine.mapping;

import com.joshsoftware.templateDrivenDocumentService.engine.definition.DocumentDefinition;
import tools.jackson.databind.JsonNode;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

// Runs the definition's compiled mapping over a payload; the mapping was fully checked at load
public final class DefaultMappingEngine implements MappingEngine {

    private final Clock clock;

    public DefaultMappingEngine(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public JsonNode transform(DocumentDefinition def, JsonNode payload) {
        // One clock reading per document, so statementDate and generatedAt agree even across midnight
        return new PayloadTransformer(LocalDateTime.now(clock), def.dateFormats())
                .mapObject(def.mapping(), payload);
    }
}
