package com.joshsoftware.templateDrivenDocumentService.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

// One S3Client for every environment: MinIO locally (endpoint override + path-style),
// AWS S3 on staging and production. Which one is decided only by tdds.storage.s3.* settings.
@Slf4j
@Configuration
@EnableConfigurationProperties(S3StorageProperties.class)
public class S3Config {

    @Bean(destroyMethod = "close")
    public S3Client s3Client(S3StorageProperties properties) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(credentialsProvider(properties))
                .forcePathStyle(properties.pathStyleAccess())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(properties.apiCallTimeout())
                        .build());
        if (properties.endpoint() != null) {
            builder.endpointOverride(properties.endpoint());
        }
        log.info("[S3Config#s3Client] {}", properties);
        return builder.build();
    }

    private static AwsCredentialsProvider credentialsProvider(S3StorageProperties properties) {
        if (properties.hasStaticCredentials()) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.accessKey(), properties.secretKey()));
        }
        return DefaultCredentialsProvider.builder().build();
    }
}
