package com.joshsoftware.templateDrivenDocumentService.config;

import com.joshsoftware.templateDrivenDocumentService.queue.QueueProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// Binds tdds.queue.*; the DB and MQ adapters register their own beans under tdds.queue.type
@Configuration
@EnableConfigurationProperties(QueueProperties.class)
public class QueueConfig {
}
