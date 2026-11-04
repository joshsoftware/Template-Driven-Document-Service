package com.joshsoftware.templateDrivenDocumentService.engine.contract;

import lombok.AllArgsConstructor;
import lombok.Getter;

// One failed contract check. Holds the field and the rule, never the value.
@Getter
@AllArgsConstructor
public class Violation {

    // Field that failed, for example transactions[3].amount
    private final String field;

    // Rule that failed, for example NOT_A_NUMBER
    private final String rule;
}
