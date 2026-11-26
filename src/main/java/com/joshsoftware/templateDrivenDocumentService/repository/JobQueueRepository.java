package com.joshsoftware.templateDrivenDocumentService.repository;

import com.joshsoftware.templateDrivenDocumentService.entity.JobQueueEntry;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

// job_queue access through native queries only (no generic save/findAll), so the blob is read just by findById
public interface JobQueueRepository extends Repository<JobQueueEntry, UUID> {

    // Inserts only for a job that exists and is still PROCESSING; returns 0 otherwise. A second insert fails on the PK.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO job_queue (job_id, payload_encrypted, key_id)
            SELECT id, :data, :keyId FROM document_jobs WHERE id = :jobId AND status = 'PROCESSING'
            """, nativeQuery = true)
    int insertIfProcessing(@Param("jobId") UUID jobId, @Param("data") byte[] data, @Param("keyId") String keyId);

    // The only read of the encrypted payload
    Optional<JobQueueEntry> findById(UUID jobId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM job_queue WHERE job_id = :jobId", nativeQuery = true)
    int deleteByJobId(@Param("jobId") UUID jobId);
}
