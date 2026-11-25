package com.joshsoftware.templateDrivenDocumentService.queue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Binds tdds.queue.* the way Spring Boot does at startup
class QueuePropertiesTest {

    @Test
    void defaultsToTheDbQueue() {
        QueueProperties properties = bind(Map.of());

        assertThat(properties.type()).isEqualTo(QueueProperties.Type.DB);
        assertThat(properties.maxAttempts()).isEqualTo(3);
        assertThat(properties.lease()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void bindsTheMqQueueAndCustomLimits() {
        QueueProperties properties = bind(Map.of(
                "tdds.queue.type", "mq",
                "tdds.queue.max-attempts", "5",
                "tdds.queue.lease", "90s"));

        assertThat(properties.type()).isEqualTo(QueueProperties.Type.MQ);
        assertThat(properties.maxAttempts()).isEqualTo(5);
        assertThat(properties.lease()).isEqualTo(Duration.ofSeconds(90));
    }

    private static QueueProperties bind(Map<String, String> values) {
        return new Binder(new MapConfigurationPropertySource(values))
                .bindOrCreate("tdds.queue", QueueProperties.class);
    }
}
