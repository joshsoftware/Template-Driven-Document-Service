package com.joshsoftware.templateDrivenDocumentService.audit;

import com.joshsoftware.templateDrivenDocumentService.annotation.Audit;
import com.joshsoftware.templateDrivenDocumentService.enums.AuditOutcome;
import com.joshsoftware.templateDrivenDocumentService.repository.DocumentJobRepository;
import com.joshsoftware.templateDrivenDocumentService.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Writes the audit row for methods annotated with @Audit.
// Lowest precedence so it runs inside the method's @Transactional (TransactionConfig orders that first).
@Slf4j
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AuditAspect {

    private final AuditLogService auditLogService;
    private final DocumentJobRepository documentJobRepository;
    private final TransactionTemplate separateTransaction;
    private final Clock clock;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNames = new DefaultParameterNameDiscoverer();
    private final Map<String, Expression> expressions = new ConcurrentHashMap<>();

    public AuditAspect(AuditLogService auditLogService, DocumentJobRepository documentJobRepository,
                       PlatformTransactionManager transactionManager, Clock clock) {
        this.auditLogService = auditLogService;
        this.documentJobRepository = documentJobRepository;
        this.separateTransaction = new TransactionTemplate(transactionManager);
        this.separateTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
    }

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            // Same transaction: it rolls back, so there is nothing to record
            if (audit.mode() == Audit.Mode.SEPARATE) {
                writeSeparately(joinPoint, audit, null, audit.outcome().resolve(false));
            }
            throw ex;
        }
        if (!conditionHolds(joinPoint, audit, result)) {
            return result;
        }
        AuditOutcome outcome = audit.outcome().resolve(true);
        if (audit.mode() == Audit.Mode.SEPARATE) {
            writeSeparately(joinPoint, audit, result, outcome);
        } else {
            // A failure here rolls back the method's change with it
            write(joinPoint, audit, result, outcome);
        }
        return result;
    }

    // The caller already has (or lost) what it asked for, so a failed write is logged, never thrown
    private void writeSeparately(ProceedingJoinPoint joinPoint, Audit audit, Object result, AuditOutcome outcome) {
        try {
            separateTransaction.executeWithoutResult(status -> write(joinPoint, audit, result, outcome));
        } catch (RuntimeException e) {
            log.error("[AuditAspect#writeSeparately] audit not written action={} outcome={} type={}",
                    audit.action(), outcome, e.getClass().getName());
        }
    }

    private void write(ProceedingJoinPoint joinPoint, Audit audit, Object result, AuditOutcome outcome) {
        EvaluationContext context = context(joinPoint, result);
        UUID jobId = evaluate(audit.jobId(), context, UUID.class);
        String channel = channel(audit.channel(), context);
        auditLogService.record(documentJobRepository.getReferenceById(jobId), audit.action(), outcome, channel, now());
    }

    private boolean conditionHolds(ProceedingJoinPoint joinPoint, Audit audit, Object result) {
        if (audit.condition().isEmpty()) {
            return true;
        }
        return Boolean.TRUE.equals(evaluate(audit.condition(), context(joinPoint, result), Boolean.class));
    }

    private String channel(String expression, EvaluationContext context) {
        if (expression.isEmpty()) {
            return null;
        }
        Object value = evaluate(expression, context, Object.class);
        if (value == null) {
            return null;
        }
        return value instanceof Enum<?> e ? e.name() : value.toString();
    }

    private <T> T evaluate(String expression, EvaluationContext context, Class<T> type) {
        return expressions.computeIfAbsent(expression, parser::parseExpression).getValue(context, type);
    }

    private EvaluationContext context(ProceedingJoinPoint joinPoint, Object result) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                null, signature.getMethod(), joinPoint.getArgs(), parameterNames);
        context.setVariable("result", result);
        return context;
    }

    // Timestamp columns are TIMESTAMPTZ written as UTC (hibernate.jdbc.time_zone)
    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
