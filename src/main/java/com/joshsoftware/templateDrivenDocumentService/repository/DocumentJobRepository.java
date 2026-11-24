package com.joshsoftware.templateDrivenDocumentService.repository;

import com.joshsoftware.templateDrivenDocumentService.entity.DocumentJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// One row per generate request, keyed by jobId
public interface DocumentJobRepository extends JpaRepository<DocumentJob, UUID> {
}
