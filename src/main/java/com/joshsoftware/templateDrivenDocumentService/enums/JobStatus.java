package com.joshsoftware.templateDrivenDocumentService.enums;

// Status of a document job. Matches the job_status type in the database.
// Names must stay the same as the database values.
public enum JobStatus {

    // Job created, document is being generated.
    PROCESSING,

    // PDF is generated and stored in S3.
    READY,

    // Generation failed. The reason is in error_code.
    FAILED,

    // PDF was removed after the retention period.
    EXPIRED
}
