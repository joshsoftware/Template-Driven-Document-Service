package com.joshsoftware.templateDrivenDocumentService.audit;

import com.joshsoftware.templateDrivenDocumentService.annotation.Audit;
import com.joshsoftware.templateDrivenDocumentService.config.TransactionConfig;
import com.joshsoftware.templateDrivenDocumentService.dto.DocumentDownload;
import com.joshsoftware.templateDrivenDocumentService.entity.AuditLog;
import com.joshsoftware.templateDrivenDocumentService.entity.DocumentJob;
import com.joshsoftware.templateDrivenDocumentService.enums.AuditAction;
import com.joshsoftware.templateDrivenDocumentService.enums.AuditOutcome;
import com.joshsoftware.templateDrivenDocumentService.enums.Channel;
import com.joshsoftware.templateDrivenDocumentService.repository.AuditLogRepository;
import com.joshsoftware.templateDrivenDocumentService.repository.DocumentJobRepository;
import com.joshsoftware.templateDrivenDocumentService.service.AuditLogService;
import com.joshsoftware.templateDrivenDocumentService.service.impl.AuditLogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

// Real proxies and transaction ordering; only the repositories and the transaction manager are fakes
@SpringJUnitConfig(AuditAspectTest.Config.class)
class AuditAspectTest {

    private static final UUID JOB_ID = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final Instant NOW = Instant.parse("2026-11-27T05:30:00Z");

    @Autowired
    private AuditedTarget target;
    @Autowired
    private AuditLogService auditLogService;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private DocumentJobRepository documentJobRepository;
    @Autowired
    private FakeTransactionManager transactionManager;

    private final List<AuditLog> saved = new ArrayList<>();
    private final List<Boolean> transactionActiveAtSave = new ArrayList<>();

    @BeforeEach
    void setUp() {
        reset(auditLogRepository);
        reset(documentJobRepository);
        transactionManager.events.clear();
        saved.clear();
        transactionActiveAtSave.clear();
        when(documentJobRepository.getReferenceById(any())).thenAnswer(inv -> {
            DocumentJob job = new DocumentJob();
            job.setId(inv.getArgument(0));
            return job;
        });
        when(auditLogRepository.save(any())).thenAnswer(inv -> {
            saved.add(inv.getArgument(0));
            transactionActiveAtSave.add(TransactionSynchronizationManager.isActualTransactionActive());
            return inv.getArgument(0);
        });
    }

    @Test
    void sameTransactionWritesTheRowInsideTheMethodsTransaction() {
        target.ready(JOB_ID, Channel.MOBILE);

        assertThat(saved).hasSize(1);
        AuditLog entry = saved.getFirst();
        assertThat(entry.getDocumentJob().getId()).isEqualTo(JOB_ID);
        assertThat(entry.getAction()).isEqualTo(AuditAction.GENERATE);
        assertThat(entry.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(entry.getChannel()).isEqualTo("MOBILE");
        assertThat(entry.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 11, 27, 5, 30));
        assertThat(transactionActiveAtSave).containsExactly(true);
        // One transaction: the method's
        assertThat(transactionManager.events).containsExactly("begin", "commit");
    }

    @Test
    void sameTransactionWritesNothingWhenTheMethodThrows() {
        assertThatThrownBy(() -> target.readyThenFail(JOB_ID)).isInstanceOf(IllegalStateException.class);

        assertThat(saved).isEmpty();
        assertThat(transactionManager.events).containsExactly("begin", "rollback");
    }

    @Test
    void sameTransactionAuditFailureRollsBackTheMethod() {
        when(auditLogRepository.save(any())).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> target.ready(JOB_ID, Channel.MOBILE)).hasMessage("db down");

        assertThat(transactionManager.events).containsExactly("begin", "rollback");
    }

    @Test
    void separateRecordsSuccessInItsOwnTransaction() throws IOException {
        target.stream(download(), false);

        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getAction()).isEqualTo(AuditAction.DOWNLOAD);
        assertThat(saved.getFirst().getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(saved.getFirst().getChannel()).isEqualTo("NET_BANKING");
        assertThat(transactionActiveAtSave).containsExactly(true);
        assertThat(transactionManager.events).containsExactly("begin", "commit");
    }

    @Test
    void separateRecordsFailureAndRethrows() {
        assertThatThrownBy(() -> target.stream(download(), true)).isInstanceOf(IOException.class);

        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getOutcome()).isEqualTo(AuditOutcome.FAILURE);
    }

    @Test
    void separateAuditFailureIsSwallowed() throws IOException {
        when(auditLogRepository.save(any())).thenThrow(new IllegalStateException("db down"));

        target.stream(download(), false);

        assertThat(transactionManager.events).containsExactly("begin", "rollback");
    }

    @Test
    void falseConditionWritesNothing() {
        assertThat(target.expire(JOB_ID, false)).isFalse();

        assertThat(saved).isEmpty();
    }

    @Test
    void trueConditionWritesTheRowWithNoChannel() {
        target.expire(JOB_ID, true);

        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getAction()).isEqualTo(AuditAction.EXPIRE);
        assertThat(saved.getFirst().getChannel()).isNull();
    }

    @Test
    void recordOutsideATransactionIsRefused() {
        assertThatThrownBy(() -> auditLogService.record(new DocumentJob(), AuditAction.GENERATE,
                AuditOutcome.SUCCESS, null, LocalDateTime.now()))
                .isInstanceOf(IllegalTransactionStateException.class);

        assertThat(saved).isEmpty();
    }

    private static DocumentDownload download() {
        return new DocumentDownload(JOB_ID, "statement.pdf", "NET_BANKING", null);
    }

    // One audited method per case the aspect handles
    static class AuditedTarget {

        @Transactional
        @Audit(action = AuditAction.GENERATE, outcome = Audit.Outcome.SUCCESS, jobId = "#jobId", channel = "#channel")
        public void ready(UUID jobId, Channel channel) {
        }

        @Transactional
        @Audit(action = AuditAction.GENERATE, outcome = Audit.Outcome.SUCCESS, jobId = "#jobId")
        public void readyThenFail(UUID jobId) {
            throw new IllegalStateException("boom");
        }

        @Audit(action = AuditAction.DOWNLOAD, jobId = "#download.jobId()", channel = "#download.channel()",
                mode = Audit.Mode.SEPARATE)
        public void stream(DocumentDownload download, boolean fail) throws IOException {
            if (fail) {
                throw new IOException("client went away");
            }
        }

        @Transactional
        @Audit(action = AuditAction.EXPIRE, outcome = Audit.Outcome.SUCCESS, jobId = "#jobId", condition = "#result")
        public boolean expire(UUID jobId, boolean changed) {
            return changed;
        }
    }

    // Tracks begin/commit/rollback and supports joining, MANDATORY and REQUIRES_NEW like a real one
    static class FakeTransactionManager extends AbstractPlatformTransactionManager {

        final List<String> events = new CopyOnWriteArrayList<>();
        private final ThreadLocal<Boolean> active = ThreadLocal.withInitial(() -> false);

        private record Tx(boolean existing) {
        }

        @Override
        protected Object doGetTransaction() {
            return new Tx(active.get());
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) {
            return ((Tx) transaction).existing();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            active.set(true);
            events.add("begin");
        }

        @Override
        protected Object doSuspend(Object transaction) {
            active.set(false);
            return Boolean.TRUE;
        }

        @Override
        protected void doResume(Object transaction, Object suspendedResources) {
            active.set(true);
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            events.add("commit");
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            events.add("rollback");
        }

        // A participant failed: the outer transaction rolls back when it completes
        @Override
        protected void doSetRollbackOnly(DefaultTransactionStatus status) {
        }

        @Override
        protected void doCleanupAfterCompletion(Object transaction) {
            active.set(false);
        }
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @Import({TransactionConfig.class, AuditAspect.class, AuditLogServiceImpl.class})
    static class Config {

        @Bean
        FakeTransactionManager transactionManager() {
            return new FakeTransactionManager();
        }

        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneId.of("Asia/Kolkata"));
        }

        @Bean
        DocumentJobRepository documentJobRepository() {
            return mock(DocumentJobRepository.class);
        }

        @Bean
        AuditLogRepository auditLogRepository() {
            return mock(AuditLogRepository.class);
        }

        @Bean
        AuditedTarget auditedTarget() {
            return new AuditedTarget();
        }
    }
}
