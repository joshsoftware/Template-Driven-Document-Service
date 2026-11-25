package com.joshsoftware.templateDrivenDocumentService.config.crypto;

// What job_queue stores: the key id and [12-byte IV][AES-256-GCM ciphertext + tag]
public record EncryptedPayload(String keyId, byte[] data) {

    // Never print the bytes, only how many there are
    @Override
    public String toString() {
        return "EncryptedPayload[keyId=%s, bytes=%d]".formatted(keyId, data == null ? 0 : data.length);
    }
}
