package com.joshsoftware.templateDrivenDocumentService.engine.contract;

// Why a payload field failed the contract; sent to the caller as the violation's rule name
public enum ViolationRule {
    // Required field is missing, null or blank
    REQUIRED,
    // Value has the wrong JSON shape, e.g. text where an object, list or string is expected
    WRONG_TYPE,
    // Number field is not a plain decimal
    NOT_A_NUMBER,
    // Date field does not match dateFormats.date, or is not a real date
    BAD_DATE,
    // Datetime field does not match dateFormats.datetime, or is not a real date and time
    BAD_DATETIME,
    // Enum field is not one of the contract's values (exact match)
    NOT_ALLOWED
}
