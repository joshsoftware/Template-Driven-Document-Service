package com.joshsoftware.templateDrivenDocumentService.constants;

// Keys added to every log line of a request.
public interface MdcConstants {

    // Set at the start of every request.
    String REQUEST_ID = "requestId";

    // Set once the job is created.
    String JOB_ID = "jobId";
}
