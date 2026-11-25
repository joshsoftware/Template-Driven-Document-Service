-- Async generation (Stage 3): one row per job waiting to be rendered, used by both the DB and MQ queues.
-- Holds the encrypted mapped payload plus the lease and attempt count; deleted when the job is READY or FAILED,
-- so a finished job leaves no customer data behind. document_jobs stays the long-lived history.

CREATE TABLE job_queue (
    job_id      UUID         NOT NULL,
    -- [12-byte IV][AES-256-GCM ciphertext + tag] of the gzipped mapped JSON, job id as AAD
    payload_enc BYTEA        NOT NULL,
    -- Names the encryption key, so keys can be rotated without breaking queued jobs
    key_id      VARCHAR(64)  NOT NULL,
    attempts    INTEGER      NOT NULL DEFAULT 0,
    -- NULL or in the past = free to claim; in the future = a consumer owns the job
    lease_until TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_job_queue PRIMARY KEY (job_id),
    CONSTRAINT fk_job_queue_document_job_id
        FOREIGN KEY (job_id) REFERENCES document_jobs (id) ON DELETE CASCADE
);

-- The DB queue's claim query takes the oldest job first
CREATE INDEX idx_job_queue_created_at ON job_queue (created_at);
