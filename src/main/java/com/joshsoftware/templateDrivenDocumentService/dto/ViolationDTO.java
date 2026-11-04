package com.joshsoftware.templateDrivenDocumentService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * One failed check in a request or payload, e.g. {@code { "field": "transactions[3].amount", "rule": "NOT_A_NUMBER" }}.
 * Carries the field path and the rule only, never the rejected value: the value may be customer data.
 **/
@Getter
@AllArgsConstructor
@Builder
public class ViolationDTO {

    /** Contract or request field path: {@code address.city}, {@code transactions[3].amount}, {@code docType}. */
    private String field;

    /** Rule that failed: {@code REQUIRED}, {@code NOT_A_NUMBER}, {@code BAD_DATE}, {@code NOT_ALLOWED}, ... */
    private String rule;
}
