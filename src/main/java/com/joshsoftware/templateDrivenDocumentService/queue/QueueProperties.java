package com.joshsoftware.templateDrivenDocumentService.queue;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

// Settings for the job queue (tdds.queue.*), shared by every adapter
@Validated
@ConfigurationProperties(prefix = "tdds.queue")
public record QueueProperties(
        // db (Postgres, default) or mq (message broker); adapters load with @ConditionalOnProperty on this
        @NotNull @DefaultValue("db") Type type,
        // A job is marked FAILED once it has been tried more than this many times
        @Min(1) @DefaultValue("3") int maxAttempts,
        // How long a consumer owns a job before another one may take it over
        @NotNull @DefaultValue("5m") Duration lease) {

    public enum Type { DB, MQ }
}
