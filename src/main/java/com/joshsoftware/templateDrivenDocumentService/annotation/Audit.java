package com.joshsoftware.templateDrivenDocumentService.annotation;

import com.joshsoftware.templateDrivenDocumentService.enums.AuditAction;
import com.joshsoftware.templateDrivenDocumentService.enums.AuditOutcome;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Writes one audit_logs row for the job when the annotated method runs (see AuditAspect).
// jobId, channel and condition are SpEL over the method's parameters (#name) and, for condition, #result.
// Works only on public methods called through a Spring proxy: not on private methods or self-calls.
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {

    AuditAction action();

    // FROM_RESULT: SUCCESS when the method returns, FAILURE when it throws (SEPARATE mode only)
    Outcome outcome() default Outcome.FROM_RESULT;

    // Must evaluate to the job's UUID
    String jobId();

    // Empty: no channel (system actions such as EXPIRE). An enum value is stored by its name.
    String channel() default "";

    // Empty: always write. Otherwise a boolean over #result, checked only when the method returns.
    String condition() default "";

    Mode mode() default Mode.SAME_TRANSACTION;

    enum Outcome {
        FROM_RESULT,
        SUCCESS,
        FAILURE;

        public AuditOutcome resolve(boolean returned) {
            return switch (this) {
                case SUCCESS -> AuditOutcome.SUCCESS;
                case FAILURE -> AuditOutcome.FAILURE;
                case FROM_RESULT -> returned ? AuditOutcome.SUCCESS : AuditOutcome.FAILURE;
            };
        }
    }

    enum Mode {
        // Row is written in the method's transaction and commits or rolls back with it; a throw writes nothing
        SAME_TRANSACTION,
        // Row is written in its own transaction after the method; a failed write is logged, never thrown
        SEPARATE
    }
}
