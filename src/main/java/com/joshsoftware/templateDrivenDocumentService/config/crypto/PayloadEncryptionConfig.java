package com.joshsoftware.templateDrivenDocumentService.config.crypto;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Binds tdds.payload.encryption.* (checked at startup) and creates the payload cipher
@Configuration
@EnableConfigurationProperties(PayloadEncryptionProperties.class)
public class PayloadEncryptionConfig {

    @Bean
    public PayloadCipher payloadCipher(PayloadEncryptionProperties properties) {
        return new PayloadCipher(properties);
    }
}
