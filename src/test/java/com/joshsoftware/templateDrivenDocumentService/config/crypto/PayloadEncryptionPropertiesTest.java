package com.joshsoftware.templateDrivenDocumentService.config.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Startup checks on the payload encryption keys; messages and toString never contain key values
class PayloadEncryptionPropertiesTest {

    private static final String V1 = PayloadCipherTest.key(1);

    @Test
    void acceptsAValid32ByteKey() {
        assertThat(new PayloadEncryptionProperties("v1", Map.of("v1", V1)).activeKeyId()).isEqualTo("v1");
    }

    @Test
    void activeKeyMustBeConfigured() {
        assertThatThrownBy(() -> new PayloadEncryptionProperties("v2", Map.of("v1", V1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'v2'");
    }

    @Test
    void unresolvedPlaceholderIsRejectedAsNotBase64() {
        // What Spring binds when TDDS_PAYLOAD_ENCRYPTION_KEY is not set
        assertThatThrownBy(() -> new PayloadEncryptionProperties("v1", Map.of("v1", "${TDDS_PAYLOAD_ENCRYPTION_KEY}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payload encryption key 'v1' is not valid base64");
    }

    @Test
    void keyMustBeExactly32Bytes() {
        String short31 = Base64.getEncoder().encodeToString(new byte[31]);

        assertThatThrownBy(() -> new PayloadEncryptionProperties("v1", Map.of("v1", short31)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payload encryption key 'v1' must be 32 bytes, got 31");
    }

    @Test
    void toStringListsKeyIdsOnly() {
        String text = new PayloadEncryptionProperties("v1", Map.of("v1", V1)).toString();

        assertThat(text).contains("activeKeyId=v1", "v1").doesNotContain(V1);
    }
}
