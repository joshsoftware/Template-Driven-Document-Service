package com.joshsoftware.templateDrivenDocumentService.exception;

import com.joshsoftware.templateDrivenDocumentService.enums.ErrorCode;

import java.util.regex.Pattern;

// No document job for the id, or the id is not a UUID. Returns 404.
public class JobNotFoundException extends AppException {

    // A UUID is 36 characters; anything longer is cut so a path value cannot flood the logs
    private static final int MAX_LOGGED_LENGTH = 36;
    private static final Pattern UNSAFE = Pattern.compile("[^A-Za-z0-9-]");

    public JobNotFoundException(String jobId) {
        super(ErrorCode.JOB_NOT_FOUND, "No document job for jobId=%s".formatted(safe(jobId)));
    }

    // The id comes from the URL: keep only UUID characters so it cannot forge log lines
    private static String safe(String jobId) {
        if (jobId == null) {
            return null;
        }
        String cut = jobId.length() > MAX_LOGGED_LENGTH ? jobId.substring(0, MAX_LOGGED_LENGTH) : jobId;
        return UNSAFE.matcher(cut).replaceAll("?");
    }
}
