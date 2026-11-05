package com.joshsoftware.templateDrivenDocumentService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// One failed validation check.
// Holds the field name and the rule only, never the value, because it may be customer data.
@Getter
@AllArgsConstructor
@Builder
public class ViolationDTO {

    // Field that failed, for example address.city
    private String field;

    // Rule that failed, for example REQUIRED
    private String rule;
}
