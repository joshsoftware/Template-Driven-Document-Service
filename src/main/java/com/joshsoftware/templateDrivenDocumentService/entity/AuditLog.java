package com.joshsoftware.templateDrivenDocumentService.entity;

import com.joshsoftware.templateDrivenDocumentService.enums.AuditAction;
import com.joshsoftware.templateDrivenDocumentService.enums.AuditOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * author : Siddhesh Datkhile
 **/

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "documentJob")
@Entity
@Table(
        name = "audit_logs",
        indexes = @Index(name = "idx_audit_logs_document_job_id", columnList = "document_job_id")
)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "document_job_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_audit_logs_document_job_id"))
    private DocumentJob documentJob;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, updatable = false, columnDefinition = "audit_action")
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, updatable = false, columnDefinition = "audit_outcome")
    private AuditOutcome outcome;

    @Column(updatable = false, length = 100)
    private String clientId;

    @Column(updatable = false, length = 20)
    private String channel;

    @Column(updatable = false)
    private LocalDateTime createdAt;
}
