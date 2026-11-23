package com.joshsoftware.templateDrivenDocumentService.service.impl;

import com.joshsoftware.templateDrivenDocumentService.exception.StorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalDocumentStorageServiceImplTest {

    @TempDir
    Path root;

    @Test
    void writesThePdfUnderTheKeyAndReturnsTheKey() throws IOException {
        LocalDocumentStorageServiceImpl storage = new LocalDocumentStorageServiceImpl(root.toString());

        String path = storage.store("statement/v1/job.pdf", new byte[]{1, 2, 3});

        assertThat(path).isEqualTo("statement/v1/job.pdf");
        assertThat(Files.readAllBytes(root.resolve(path))).containsExactly(1, 2, 3);
        // No temp files left behind
        try (var files = Files.list(root.resolve("statement/v1"))) {
            assertThat(files).hasSize(1);
        }
    }

    @Test
    void rejectsAKeyOutsideTheRoot() {
        LocalDocumentStorageServiceImpl storage = new LocalDocumentStorageServiceImpl(root.toString());

        assertThatThrownBy(() -> storage.store("../escape.pdf", new byte[]{1}))
                .isInstanceOf(StorageException.class);
    }
}
