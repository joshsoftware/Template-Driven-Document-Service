package com.joshsoftware.templateDrivenDocumentService.engine.definition;

import java.util.UUID;

// Active definition for a template; see DbDocumentDefinitionRegistry for the DB-backed implementation
public interface DocumentDefinitionRegistry {

    // Throws when there is no ACTIVE version or its config is broken
    DocumentDefinition get(String templateId);

    // Exact version by its row id, whatever its status, so a queued job renders with the version it was validated against
    DocumentDefinition getByVersionId(UUID templateVersionId);
}
