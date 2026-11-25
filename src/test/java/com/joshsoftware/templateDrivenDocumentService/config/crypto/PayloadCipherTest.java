package com.joshsoftware.templateDrivenDocumentService.config.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// AES-256-GCM payload cipher: round trip, fresh IV, and every tamper case fails closed
class PayloadCipherTest {

    private static final String V1 = key(1);
    private static final String V2 = key(2);
    private static final byte[] PAYLOAD = """
            {"customerName":"TEST CUSTOMER","accountNumber":"10000000000001","transactions":[]}
            """.getBytes(StandardCharsets.UTF_8);

    private final UUID jobId = UUID.randomUUID();
    private final PayloadCipher cipher = new PayloadCipher(new PayloadEncryptionProperties("v1", Map.of("v1", V1)));

    @Test
    void decryptsWhatItEncrypted() {
        EncryptedPayload sealed = cipher.encrypt(jobId, PAYLOAD);

        assertThat(sealed.keyId()).isEqualTo("v1");
        assertThat(cipher.decrypt(jobId, sealed)).isEqualTo(PAYLOAD);
    }

    @Test
    void storedBytesHoldNoPlaintext() {
        byte[] data = cipher.encrypt(jobId, PAYLOAD).data();

        assertThat(new String(data, StandardCharsets.ISO_8859_1)).doesNotContain("TEST CUSTOMER", "10000000000001");
    }

    @Test
    void sameInputEncryptsDifferentlyEachTime() {
        assertThat(cipher.encrypt(jobId, PAYLOAD).data()).isNotEqualTo(cipher.encrypt(jobId, PAYLOAD).data());
    }

    @Test
    void largePayloadIsCompressed() {
        byte[] large = "{\"row\":\"same text\"},".repeat(5000).getBytes(StandardCharsets.UTF_8);

        EncryptedPayload sealed = cipher.encrypt(jobId, large);

        assertThat(sealed.data().length).isLessThan(large.length / 10);
        assertThat(cipher.decrypt(jobId, sealed)).isEqualTo(large);
    }

    @Test
    void changedByteFails() {
        EncryptedPayload sealed = cipher.encrypt(jobId, PAYLOAD);
        byte[] tampered = sealed.data().clone();
        tampered[tampered.length - 1] ^= 1;

        assertThatThrownBy(() -> cipher.decrypt(jobId, new EncryptedPayload("v1", tampered)))
                .isInstanceOf(PayloadEncryptionException.class)
                .hasMessage("Job payload could not be decrypted");
    }

    @Test
    void anotherJobsIdFails() {
        EncryptedPayload sealed = cipher.encrypt(jobId, PAYLOAD);

        assertThatThrownBy(() -> cipher.decrypt(UUID.randomUUID(), sealed))
                .isInstanceOf(PayloadEncryptionException.class);
    }

    @Test
    void unknownKeyIdFails() {
        EncryptedPayload sealed = cipher.encrypt(jobId, PAYLOAD);

        assertThatThrownBy(() -> cipher.decrypt(jobId, new EncryptedPayload("v9", sealed.data())))
                .isInstanceOf(PayloadEncryptionException.class)
                .hasMessageContaining("v9");
    }

    @Test
    void truncatedDataFails() {
        assertThatThrownBy(() -> cipher.decrypt(jobId, new EncryptedPayload("v1", new byte[10])))
                .isInstanceOf(PayloadEncryptionException.class)
                .hasMessage("Job payload is truncated");
    }

    @Test
    void payloadFromTheOldKeyStillDecryptsAfterRotation() {
        EncryptedPayload underV1 = cipher.encrypt(jobId, PAYLOAD);
        PayloadCipher rotated = new PayloadCipher(new PayloadEncryptionProperties("v2", Map.of("v1", V1, "v2", V2)));

        assertThat(rotated.decrypt(jobId, underV1)).isEqualTo(PAYLOAD);
        assertThat(rotated.encrypt(jobId, PAYLOAD).keyId()).isEqualTo("v2");
    }

    @Test
    void toStringShowsNoBytes() {
        EncryptedPayload sealed = cipher.encrypt(jobId, PAYLOAD);

        assertThat(sealed.toString()).isEqualTo("EncryptedPayload[keyId=v1, bytes=%d]".formatted(sealed.data().length));
    }

    // A test-only 32-byte key filled with one value
    static String key(int fill) {
        byte[] bytes = new byte[PayloadEncryptionProperties.KEY_BYTES];
        java.util.Arrays.fill(bytes, (byte) fill);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
