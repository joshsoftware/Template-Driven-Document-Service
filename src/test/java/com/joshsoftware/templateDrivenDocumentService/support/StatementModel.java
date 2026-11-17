package com.joshsoftware.templateDrivenDocumentService.support;

import com.joshsoftware.templateDrivenDocumentService.engine.contract.DefaultContractValidator;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.DateFormats;
import com.joshsoftware.templateDrivenDocumentService.engine.definition.DocumentDefinition;

import java.util.Map;

// Typed statement models from samples/model*.json, built by the real validator so there is one way to make a model
public final class StatementModel {

    // The model samples are written with ISO dates, not the bank's yyyyMMdd
    private static final DateFormats ISO = DateFormats.of("yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ss");

    private StatementModel() {
    }

    public static Map<String, Object> load(String sample) {
        DocumentDefinition statement = StatementDefinition.load();
        DocumentDefinition isoDates = new DocumentDefinition(statement.templateVersionId(), statement.templateId(),
                statement.version(), statement.templateHtml(), statement.contract(), ISO, statement.mapping());
        return new DefaultContractValidator().validate(isoDates, Fixtures.json(sample));
    }
}
