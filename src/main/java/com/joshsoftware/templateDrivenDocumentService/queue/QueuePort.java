package com.joshsoftware.templateDrivenDocumentService.queue;

import java.util.UUID;

// Publishing side of the job queue; the adapter is picked by tdds.queue.type (db or mq).
// The message carries only the job id: customer data stays encrypted on the job row, never on the queue.
public interface QueuePort {

    // Called only after the job row and its payload are committed; throws if the job could not be published
    void enqueue(UUID jobId);
}
