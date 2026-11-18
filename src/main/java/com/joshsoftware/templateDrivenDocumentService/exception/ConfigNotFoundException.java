package com.joshsoftware.templateDrivenDocumentService.exception;

import com.joshsoftware.templateDrivenDocumentService.enums.ErrorCode;

// No active template for the template_id. Returns 404.
public class ConfigNotFoundException extends AppException {

    public ConfigNotFoundException(String templateId) {
        super(ErrorCode.CONFIG_NOT_FOUND, "No ACTIVE template for templateId=%s".formatted(templateId));
    }
}
