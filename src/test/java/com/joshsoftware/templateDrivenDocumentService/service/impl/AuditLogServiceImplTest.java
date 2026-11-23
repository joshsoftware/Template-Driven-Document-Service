package com.joshsoftware.templateDrivenDocumentService.service.impl;

import com.joshsoftware.templateDrivenDocumentService.entity.AuditLog;
import com.joshsoftware.templateDrivenDocumentService.entity.DocumentJob;
import com.joshsoftware.templateDrivenDocumentService.enums.AuditAction;
import com.joshsoftware.templateDrivenDocumentService.enums.AuditOutcome;
import com.joshsoftware.templateDrivenDocumentService.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditLogServiceImplTest {

    private final AuditLogRepository repository = mock(AuditLogRepository.class);
    private final AuditLogServiceImpl service = new AuditLogServiceImpl(repository);

    @Test
    void savesOneRowWithTheGivenValues() {
        DocumentJob job = new DocumentJob();
        LocalDateTime at = LocalDateTime.of(2024, 6, 1, 4, 30);

        service.record(job, AuditAction.GENERATE, AuditOutcome.SUCCESS, "MOBILE", at);

        ArgumentCaptor<AuditLog> saved = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(saved.capture());
        AuditLog entry = saved.getValue();
        assertThat(entry.getDocumentJob()).isSameAs(job);
        assertThat(entry.getAction()).isEqualTo(AuditAction.GENERATE);
        assertThat(entry.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(entry.getChannel()).isEqualTo("MOBILE");
        assertThat(entry.getCreatedAt()).isEqualTo(at);
        // Stage 1 has no caller authentication
        assertThat(entry.getClientId()).isNull();
    }
}
