package com.joshsoftware.templateDrivenDocumentService.config.crypto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Base64;
import java.util.Map;

// Keys for the job payload encryption (tdds.payload.encryption.*), from AWS Secrets Manager via env, .env locally.
// Several keys may be listed for rotation: new payloads use activeKeyId, queued ones decrypt with their own key id.
// Checked at startup, so a missing or malformed key fails fast instead of on the first request.
@Validated
@ConfigurationProperties(prefix = "tdds.payload.encryption")
public record PayloadEncryptionProperties(@NotBlank String activeKeyId, @NotEmpty Map<String, String> keys) {

    // AES-256 needs exactly 32 bytes
    static final int KEY_BYTES = 32;

    public PayloadEncryptionProperties {
        if (keys != null && activeKeyId != null) {
            if (!keys.containsKey(activeKeyId)) {
                throw new IllegalArgumentException("No payload encryption key configured for active key id '%s'"
                        .formatted(activeKeyId));
            }
            // Messages name the key id only, never the key value
            keys.forEach(PayloadEncryptionProperties::checkKey);
            keys = Map.copyOf(keys);
        }
    }

    private static void checkKey(String keyId, String base64) {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(base64 == null ? "" : base64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Payload encryption key '%s' is not valid base64".formatted(keyId));
        }
        if (key.length != KEY_BYTES) {
            throw new IllegalArgumentException("Payload encryption key '%s' must be %d bytes, got %d"
                    .formatted(keyId, KEY_BYTES, key.length));
        }
    }

    // Keys are secrets: list the ids only
    @Override
    public String toString() {
        return "PayloadEncryptionProperties[activeKeyId=%s, keyIds=%s]"
                .formatted(activeKeyId, keys == null ? null : keys.keySet());
    }
}
