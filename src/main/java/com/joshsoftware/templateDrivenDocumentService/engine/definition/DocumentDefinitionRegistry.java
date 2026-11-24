package com.joshsoftware.templateDrivenDocumentService.engine.definition;

// Active definition for a template; see DbDocumentDefinitionRegistry for the DB-backed implementation
public interface DocumentDefinitionRegistry {

    // Throws when there is no ACTIVE version or its config is broken
    DocumentDefinition get(String templateId);
}
