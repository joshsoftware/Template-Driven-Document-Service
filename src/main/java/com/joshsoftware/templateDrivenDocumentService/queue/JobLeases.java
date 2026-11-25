package com.joshsoftware.templateDrivenDocumentService.queue;

import java.util.UUID;

// Lease on a job row, so only one consumer works on a job at a time whatever the adapter.
// Attempts are counted here, so retries and FAILED are decided in the DB, not by the broker.
public interface JobLeases {

    // True when the job is PROCESSING and not leased by anyone else: lease taken and attempts + 1.
    // False when the job is finished or another consumer holds a live lease: drop the delivery.
    boolean tryAcquire(UUID jobId);

    // Drops this consumer's lease after handle() threw, so a redelivery can take it at once
    void release(UUID jobId);
}
