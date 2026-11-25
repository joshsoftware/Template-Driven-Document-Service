package com.joshsoftware.templateDrivenDocumentService.queue;

import java.util.UUID;

// Consuming side of the job queue: the adapter calls it once it holds the job's lease (JobLeases.tryAcquire).
// Delivery is at least once, so it must be idempotent: a job that is no longer PROCESSING is a no-op.
public interface JobHandler {

    // Returns when the job is finished (READY, FAILED or nothing to do): the adapter acks.
    // Throws only for an infrastructure error before the job is marked: the adapter retries later.
    void handle(UUID jobId);
}
