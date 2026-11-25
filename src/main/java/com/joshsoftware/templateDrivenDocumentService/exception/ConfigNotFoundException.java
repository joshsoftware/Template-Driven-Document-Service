package com.joshsoftware.templateDrivenDocumentService.exception;

import com.joshsoftware.templateDrivenDocumentService.enums.ErrorCode;

import java.util.UUID;

// No active template for the template_id, or no template row for a version id. Returns 404.
public class ConfigNotFoundException extends AppException {

    public ConfigNotFoundException(String templateId) {
        super(ErrorCode.CONFIG_NOT_FOUND, "No ACTIVE template for templateId=%s".formatted(templateId));
    }

    public ConfigNotFoundException(UUID templateVersionId) {
        super(ErrorCode.CONFIG_NOT_FOUND, "No template version for id=%s".formatted(templateVersionId));
    }
}
