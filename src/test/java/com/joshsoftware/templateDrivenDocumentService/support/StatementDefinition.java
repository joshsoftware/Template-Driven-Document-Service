package com.joshsoftware.templateDrivenDocumentService.support;

import com.joshsoftware.templateDrivenDocumentService.engine.definition.DocumentDefinition;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.DocumentDefinitionParser;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

// The statement v1 definition, loaded from the test fixtures through the real parser
public final class StatementDefinition {

    public static final UUID VERSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private StatementDefinition() {
    }

    public static DocumentDefinition load() {
        return new DocumentDefinitionParser(JsonMapper.builder().build())
                .parse(VERSION_ID, "statement", 1, TestTemplateInliner.inlinedStatementTemplate(),
                        Fixtures.text("contract.json"), Fixtures.text("mapping.json"));
    }
}
