package com.joshsoftware.templateDrivenDocumentService.enums;

// Status of a template version. Matches the template_status type in the database.
// Names must stay the same as the database values.
public enum TemplateStatus {

    // Uploaded but not in use yet.
    DRAFT,

    // Used for new documents. Only one active version per document type.
    ACTIVE,

    // Turned off. No longer used for new documents.
    DISABLED
}
