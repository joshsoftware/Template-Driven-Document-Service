package com.joshsoftware.templateDrivenDocumentService.service.impl;

import com.joshsoftware.templateDrivenDocumentService.config.S3StorageProperties;
import com.joshsoftware.templateDrivenDocumentService.exception.StorageException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3DocumentStorageServiceImplTest {

    private static final String KEY = "statement/v1/job.pdf";
    private static final byte[] PDF = {'%', 'P', 'D', 'F'};

    private final S3Client s3Client = mock(S3Client.class);

    private static S3StorageProperties properties(String sse) {
        return new S3StorageProperties("documents", "ap-south-1", null, false, null, null, sse,
                Duration.ofSeconds(30));
    }

    @Test
    void putsPdfUnderKeyAndReturnsKey() {
        S3DocumentStorageServiceImpl storage = new S3DocumentStorageServiceImpl(s3Client, properties("AES256"));

        String path = storage.store(KEY, PDF);

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        assertThat(path).isEqualTo(KEY);
        assertThat(request.getValue().bucket()).isEqualTo("documents");
        assertThat(request.getValue().key()).isEqualTo(KEY);
        assertThat(request.getValue().contentType()).isEqualTo("application/pdf");
        assertThat(request.getValue().contentLength()).isEqualTo(PDF.length);
        assertThat(request.getValue().serverSideEncryption()).isEqualTo(ServerSideEncryption.AES256);
    }

    @Test
    void sendsNoEncryptionHeaderWhenNotConfigured() {
        S3DocumentStorageServiceImpl storage = new S3DocumentStorageServiceImpl(s3Client, properties(null));

        storage.store(KEY, PDF);

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().serverSideEncryption()).isNull();
    }

    @Test
    void wrapsSdkFailureInStorageException() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("connection refused"));
        S3DocumentStorageServiceImpl storage = new S3DocumentStorageServiceImpl(s3Client, properties(null));

        assertThatThrownBy(() -> storage.store(KEY, PDF))
                .isInstanceOf(StorageException.class)
                .hasCauseInstanceOf(SdkClientException.class);
    }
}
