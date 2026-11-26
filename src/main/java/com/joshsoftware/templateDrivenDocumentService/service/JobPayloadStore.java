package com.joshsoftware.templateDrivenDocumentService.service;

import tools.jackson.databind.JsonNode;

import java.util.UUID;

// The mapped payload of a job waiting to be rendered, encrypted in its job_queue row.
// Joins the caller's transaction: submit() saves then commits; markReady / markFailed clear in theirs.
public interface JobPayloadStore {

    // Encrypts and inserts the job_queue row; only for a job that exists and is PROCESSING, and only once
    void save(UUID jobId, JsonNode mapped);

    // Decrypts the queued payload; JobPayloadException when there is none, PayloadEncryptionException when tampered
    JsonNode load(UUID jobId);

    // Deletes the row (payload, lease and attempts together); a no-op when there is none
    void clear(UUID jobId);
}
