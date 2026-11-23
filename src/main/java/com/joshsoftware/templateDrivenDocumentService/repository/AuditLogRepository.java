package com.joshsoftware.templateDrivenDocumentService.repository;

import com.joshsoftware.templateDrivenDocumentService.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// Append-only audit trail: rows are inserted, never updated or deleted
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
