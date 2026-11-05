package com.joshsoftware.templateDrivenDocumentService.entity;

import com.joshsoftware.templateDrivenDocumentService.enums.JobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * author : Siddhesh Datkhile
 **/

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "template")
@Entity
@Table(
        name = "document_jobs",
        indexes = @Index(name = "idx_document_jobs_template_version_id", columnList = "template_version_id")
)
public class DocumentJob {

    // jobId, assigned by the service
    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "template_version_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_document_jobs_template_version_id"))
    private Template template;

    private LocalDate periodFrom;

    private LocalDate periodTo;

    @Column(length = 20)
    private String channel;

    @Column(length = 100)
    private String requestId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "job_status")
    private JobStatus status;

    @Column(length = 50)
    private String errorCode;

    @Column(length = 512)
    private String storagePath;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    private LocalDateTime expiresAt;
}
