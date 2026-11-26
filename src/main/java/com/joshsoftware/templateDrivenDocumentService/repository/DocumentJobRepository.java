package com.joshsoftware.templateDrivenDocumentService.repository;

import com.joshsoftware.templateDrivenDocumentService.dto.JobDownloadView;
import com.joshsoftware.templateDrivenDocumentService.entity.DocumentJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

// One row per generate request, keyed by jobId
public interface DocumentJobRepository extends JpaRepository<DocumentJob, UUID> {

    // Download lookup: job status, storage path, channel and template id in one query (no template HTML)
    @Query("""
            select new com.joshsoftware.templateDrivenDocumentService.dto.JobDownloadView(
                j.id, j.status, j.storagePath, j.channel, t.templateId)
            from DocumentJob j join j.template t
            where j.id = :jobId
            """)
    Optional<JobDownloadView> findDownloadView(@Param("jobId") UUID jobId);
}
