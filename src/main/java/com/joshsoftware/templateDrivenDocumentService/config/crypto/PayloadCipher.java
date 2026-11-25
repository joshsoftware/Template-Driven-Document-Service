package com.joshsoftware.templateDrivenDocumentService.config.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

// AES-256-GCM for the queued job payload: gzip, then encrypt with a fresh random IV.
// The job id is the AAD, so a ciphertext copied onto another job fails to decrypt.
// Thread-safe: a new Cipher per call. Plain Java; PayloadEncryptionConfig creates the bean.
public final class PayloadCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final String activeKeyId;
    private final Map<String, SecretKey> keys = new HashMap<>();
    private final SecureRandom random;

    public PayloadCipher(PayloadEncryptionProperties properties) {
        this(properties, new SecureRandom());
    }

    PayloadCipher(PayloadEncryptionProperties properties, SecureRandom random) {
        this.activeKeyId = properties.activeKeyId();
        properties.keys().forEach((id, base64) ->
                keys.put(id, new SecretKeySpec(Base64.getDecoder().decode(base64.trim()), "AES")));
        this.random = random;
    }

    public EncryptedPayload encrypt(UUID jobId, byte[] plaintext) {
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keys.get(activeKeyId), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aad(jobId));
            byte[] sealed = cipher.doFinal(gzip(plaintext));
            byte[] data = ByteBuffer.allocate(IV_BYTES + sealed.length).put(iv).put(sealed).array();
            return new EncryptedPayload(activeKeyId, data);
        } catch (GeneralSecurityException | IOException e) {
            throw new PayloadEncryptionException("Job payload could not be encrypted", e);
        }
    }

    public byte[] decrypt(UUID jobId, EncryptedPayload payload) {
        SecretKey key = keys.get(payload.keyId());
        if (key == null) {
            throw new PayloadEncryptionException(
                    "No payload encryption key configured for key id '%s'".formatted(payload.keyId()));
        }
        byte[] data = payload.data();
        if (data == null || data.length < IV_BYTES + TAG_BITS / 8) {
            throw new PayloadEncryptionException("Job payload is truncated");
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, data, 0, IV_BYTES));
            cipher.updateAAD(aad(jobId));
            return gunzip(cipher.doFinal(data, IV_BYTES, data.length - IV_BYTES));
        } catch (GeneralSecurityException | IOException e) {
            // Wrong key, wrong job id or tampered bytes all end here (GCM tag check)
            throw new PayloadEncryptionException("Job payload could not be decrypted", e);
        }
    }

    // The job id's 16 bytes, binding the ciphertext to its job
    private static byte[] aad(UUID jobId) {
        return ByteBuffer.allocate(16)
                .putLong(jobId.getMostSignificantBits())
                .putLong(jobId.getLeastSignificantBits())
                .array();
    }

    private static byte[] gzip(byte[] plaintext) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
            gz.write(plaintext);
        }
        return out.toByteArray();
    }

    private static byte[] gunzip(byte[] compressed) throws IOException {
        try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            return gz.readAllBytes();
        }
    }
}
