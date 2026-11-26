package com.joshsoftware.templateDrivenDocumentService.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

// One job waiting to be rendered: encrypted payload, lease and attempts (V3 job_queue).
// Read-only for JPA: rows are written only by JobQueueRepository's native queries.
@Entity
@Table(name = "job_queue")
@Getter
@ToString
public class JobQueueEntry {

    @Id
    @Column(name = "job_id", insertable = false, updatable = false)
    private UUID jobId;

    // [IV][AES-256-GCM ciphertext + tag]; customer data, never printed
    @ToString.Exclude
    @Column(name = "payload_encrypted", insertable = false, updatable = false)
    private byte[] payloadEncrypted;

    @Column(name = "key_id", length = 64, insertable = false, updatable = false)
    private String keyId;

    @Column(insertable = false, updatable = false)
    private int attempts;

    @Column(insertable = false, updatable = false)
    private LocalDateTime leaseUntil;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
