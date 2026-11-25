package com.joshsoftware.templateDrivenDocumentService.queue;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

// Behaviour every queue adapter (DB, MQ) must show. Extend it, wire the adapter to `handler`,
// and implement the four hooks. The adapter must call JobLeases.tryAcquire before handle().
public abstract class QueuePortContractTest {

    // Handler the adapter must deliver to; finishing a job takes it out of PROCESSING
    protected final RecordingJobHandler handler = new RecordingJobHandler(this::markFinished);

    // The adapter under test
    protected abstract QueuePort queuePort();

    // A committed job row in PROCESSING, the state submit() leaves it in before enqueue
    protected abstract void createCommittedJob(UUID jobId);

    // The job leaves PROCESSING (as markReady does), so later deliveries must be no-ops
    protected abstract void markFinished(UUID jobId);

    // Lets the adapter deliver everything it has, including due retries, and returns when settled
    protected abstract void deliverPending();

    @Test
    void enqueuedJobIsHandledOnce() {
        UUID jobId = UUID.randomUUID();
        createCommittedJob(jobId);

        queuePort().enqueue(jobId);
        deliverPending();

        assertThat(handler.calls()).containsExactly(jobId);
    }

    @Test
    void jobIsDeliveredAgainWhenHandleThrows() {
        UUID jobId = UUID.randomUUID();
        createCommittedJob(jobId);
        handler.failOnce(jobId);

        queuePort().enqueue(jobId);
        deliverPending();
        deliverPending();

        assertThat(handler.calls()).containsExactly(jobId, jobId);
        assertThat(handler.finished()).contains(jobId);
    }

    @Test
    void duplicateDeliveryOfAFinishedJobIsANoOp() {
        UUID jobId = UUID.randomUUID();
        createCommittedJob(jobId);

        queuePort().enqueue(jobId);
        deliverPending();
        queuePort().enqueue(jobId);
        deliverPending();

        assertThat(handler.calls()).containsExactly(jobId);
    }

    @Test
    void unknownJobIsNeverHandled() {
        queuePort().enqueue(UUID.randomUUID());
        deliverPending();

        assertThat(handler.calls()).isEmpty();
    }

    // Records every handle() call; can fail the first call for a job to simulate an infrastructure error
    public static final class RecordingJobHandler implements JobHandler {

        private final Consumer<UUID> finish;
        private final List<UUID> calls = new CopyOnWriteArrayList<>();
        private final Set<UUID> failOnce = ConcurrentHashMap.newKeySet();
        private final Set<UUID> finished = ConcurrentHashMap.newKeySet();

        RecordingJobHandler(Consumer<UUID> finish) {
            this.finish = finish;
        }

        @Override
        public void handle(UUID jobId) {
            calls.add(jobId);
            if (failOnce.remove(jobId)) {
                throw new IllegalStateException("simulated infrastructure failure");
            }
            finish.accept(jobId);
            finished.add(jobId);
        }

        public void failOnce(UUID jobId) {
            failOnce.add(jobId);
        }

        public List<UUID> calls() {
            return List.copyOf(calls);
        }

        public Set<UUID> finished() {
            return Set.copyOf(finished);
        }
    }
}
