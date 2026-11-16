package com.joshsoftware.templateDrivenDocumentService.enums;

// Action recorded in the audit log. Matches the audit_action type in the database.
// Names must stay the same as the database values.
public enum AuditAction {

    // Document was generated. The only action used in Stage 1.
    GENERATE,

    // Document was downloaded.
    DOWNLOAD,

    // Document was removed after the retention period.
    EXPIRE
}
