package com.joshsoftware.templateDrivenDocumentService.repository;

import com.joshsoftware.templateDrivenDocumentService.entity.Template;
import com.joshsoftware.templateDrivenDocumentService.enums.TemplateStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// Template versions. Rows are write-once (trg_templates_write_once); only the status may change.
public interface TemplateRepository extends JpaRepository<Template, UUID> {

    // At most one ACTIVE version per templateId (uq_templates_one_active), so this is unique
    Optional<Template> findByTemplateIdAndStatus(String templateId, TemplateStatus status);
}
