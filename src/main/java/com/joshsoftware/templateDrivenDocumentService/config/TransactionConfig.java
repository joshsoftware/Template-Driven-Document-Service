package com.joshsoftware.templateDrivenDocumentService.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

// Puts @Transactional outside every other advice, so AuditAspect writes its row inside the method's transaction.
// Replaces Spring Boot's default setup, which leaves the order undefined.
@Configuration
@EnableTransactionManagement(proxyTargetClass = true, order = 0)
public class TransactionConfig {
}
