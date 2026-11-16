package com.joshsoftware.templateDrivenDocumentService.engine.mapping;

import com.joshsoftware.templateDrivenDocumentService.engine.definition.DocumentDefinition;
import tools.jackson.databind.JsonNode;

import java.time.Clock;
import java.util.Objects;

// Runs mapping.json's transform: path, system.*(), list {from,map}, pick-one {from,select,map}, null
public final class DefaultMappingEngine implements MappingEngine {

    private final Clock clock;

    public DefaultMappingEngine(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void checkConfig(DocumentDefinition def) {
        new MappingConfigChecker(SystemFunctions.readAt(clock), def.dateFormats())
                .checkObject(def.transform(), def.contract(), "");
    }

    @Override
    public JsonNode transform(DocumentDefinition def, JsonNode payload) {
        return new PayloadTransformer(SystemFunctions.readAt(clock), def.dateFormats())
                .mapObject(def.transform(), payload, "");
    }
}
