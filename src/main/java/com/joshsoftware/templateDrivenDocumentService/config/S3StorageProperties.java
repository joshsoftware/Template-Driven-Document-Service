package com.joshsoftware.templateDrivenDocumentService.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

// Settings for the S3 document store (tdds.storage.s3.*).
// Local development points endpoint at MinIO with path-style access and static keys.
// Staging and production leave endpoint and keys empty: the client talks to AWS S3 and takes
// credentials from the default provider chain (IAM role / task role).
// Validated at startup: an unset TDDS_STORAGE_S3_BUCKET would otherwise bind as the literal
// text "${TDDS_STORAGE_S3_BUCKET}" and only fail on the first upload.
@Validated
@ConfigurationProperties(prefix = "tdds.storage.s3")
public record S3StorageProperties(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$",
                message = "must be a valid S3 bucket name (3-63 lowercase letters, digits, dots, hyphens)")
        String bucket,
        @DefaultValue("ap-south-1") String region,
        // Only for S3-compatible stores such as MinIO; empty means AWS S3
        URI endpoint,
        @DefaultValue("false") boolean pathStyleAccess,
        // Only for MinIO; empty means the AWS default credentials provider chain
        String accessKey,
        String secretKey,
        // AES256 or aws:kms; empty sends no encryption header (MinIO without KMS rejects it)
        String serverSideEncryption,
        @DefaultValue("30s") Duration apiCallTimeout) {

    public boolean hasStaticCredentials() {
        return accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank();
    }

    // Keys are secrets: never print them
    @Override
    public String toString() {
        return "S3StorageProperties[bucket=%s, region=%s, endpoint=%s, pathStyleAccess=%s, staticCredentials=%s]"
                .formatted(bucket, region, endpoint, pathStyleAccess, hasStaticCredentials());
    }
}
