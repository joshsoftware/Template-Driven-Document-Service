package com.joshsoftware.templateDrivenDocumentService.service.impl;

import com.joshsoftware.templateDrivenDocumentService.exception.StorageException;
import com.joshsoftware.templateDrivenDocumentService.service.DocumentStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

// Writes PDFs to a local folder. Stand-in until the S3 implementation is added; not for production,
// since files on a container's disk are lost on restart and are not shared between instances.
@Slf4j
@Service
public class LocalDocumentStorageServiceImpl implements DocumentStorageService {

    private final Path root;

    public LocalDocumentStorageServiceImpl(@Value("${tdds.storage.local.root:./storage}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public String store(String key, byte[] pdf) {
        Path target = root.resolve(key).normalize();
        // Keys are built from validated ids, but never let one escape the root
        if (!target.startsWith(root)) {
            throw new StorageException("Storage key resolves outside the storage root", null);
        }
        try {
            Files.createDirectories(target.getParent());
            // Write to a temp file first, so a reader never sees a half-written PDF
            Path temp = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
            try {
                Files.write(temp, pdf);
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new StorageException("Could not write PDF to local storage", e);
        }
        log.debug("[LocalDocumentStorageServiceImpl#store] stored key={} bytes={}", key, pdf.length);
        return key;
    }
}
