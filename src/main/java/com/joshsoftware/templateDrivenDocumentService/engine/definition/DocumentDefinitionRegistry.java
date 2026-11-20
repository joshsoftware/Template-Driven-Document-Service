package com.joshsoftware.templateDrivenDocumentService.engine.definition;

// Active definition for a template; the DB-backed implementation lives in the app
public interface DocumentDefinitionRegistry {

    // Throws when there is no ACTIVE version or its config is broken
    DocumentDefinition get(String templateId);
}
