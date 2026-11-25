package com.joshsoftware.templateDrivenDocumentService.config.crypto;

// Payload could not be encrypted or decrypted (unknown key id, tampered or truncated data).
// Fixed messages only: never payload bytes, IVs or key material.
public class PayloadEncryptionException extends RuntimeException {

    public PayloadEncryptionException(String message) {
        super(message);
    }

    public PayloadEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
