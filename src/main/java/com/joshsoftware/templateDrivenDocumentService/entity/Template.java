package com.joshsoftware.templateDrivenDocumentService.entity;

import com.joshsoftware.templateDrivenDocumentService.enums.TemplateStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@Entity
@Table(
        name = "templates",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_templates_template_id_version",
                columnNames = {"template_id", "version"})
)
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, updatable = false, length = 64)
    private String templateId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, updatable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "template_status")
    private TemplateStatus status;

    @Column(updatable = false, columnDefinition = "TEXT")
    private String templateHtml;

    @Column(updatable = false, columnDefinition = "CHAR(64)")
    private String templateSha256;

    @Column(updatable = false, columnDefinition = "TEXT")
    private String contractJson;

    @Column(updatable = false, columnDefinition = "CHAR(64)")
    private String contractSha256;

    @Column(updatable = false, columnDefinition = "TEXT")
    private String mappingJson;

    @Column(updatable = false, columnDefinition = "CHAR(64)")
    private String mappingSha256;

    @Column(updatable = false, length = 100)
    private String createdBy;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime activatedAt;

    private LocalDateTime disabledAt;
}
