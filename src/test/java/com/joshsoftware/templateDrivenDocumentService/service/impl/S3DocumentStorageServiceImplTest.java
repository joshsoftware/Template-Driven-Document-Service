package com.joshsoftware.templateDrivenDocumentService.service.impl;

import com.joshsoftware.templateDrivenDocumentService.config.S3StorageProperties;
import com.joshsoftware.templateDrivenDocumentService.dto.StoredDocument;
import com.joshsoftware.templateDrivenDocumentService.exception.DocumentMissingException;
import com.joshsoftware.templateDrivenDocumentService.exception.StorageException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import java.io.ByteArrayInputStream;
import java.io.IOException;

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

    @Test
    void openReturnsTheStoredBytesAndLength() throws IOException {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(body(PDF.length));
        S3DocumentStorageServiceImpl storage = new S3DocumentStorageServiceImpl(s3Client, properties(null));

        try (StoredDocument document = storage.open(KEY)) {
            assertThat(document.content().readAllBytes()).isEqualTo(PDF);
            assertThat(document.contentLength()).isEqualTo(PDF.length);
        }
        ArgumentCaptor<GetObjectRequest> request = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo("documents");
        assertThat(request.getValue().key()).isEqualTo(KEY);
    }

    @Test
    void openReportsUnknownLengthAsMinusOne() {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(body(null));
        S3DocumentStorageServiceImpl storage = new S3DocumentStorageServiceImpl(s3Client, properties(null));

        assertThat(storage.open(KEY).contentLength()).isEqualTo(-1);
    }

    @Test
    void openOfAMissingKeyIsDocumentMissing() {
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().statusCode(404).message("missing").build());
        S3DocumentStorageServiceImpl storage = new S3DocumentStorageServiceImpl(s3Client, properties(null));

        assertThatThrownBy(() -> storage.open(KEY))
                .isInstanceOf(DocumentMissingException.class)
                .hasMessage("Stored PDF is missing");
    }

    // MinIO can answer a missing object with a plain 404 instead of NoSuchKey
    @Test
    void openWithA404IsDocumentMissing() {
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).message("not found").build());
        S3DocumentStorageServiceImpl storage = new S3DocumentStorageServiceImpl(s3Client, properties(null));

        assertThatThrownBy(() -> storage.open(KEY)).isInstanceOf(DocumentMissingException.class);
    }

    @Test
    void openWithAnS3ErrorIsStorageException() {
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(500).message("internal").build());
        S3DocumentStorageServiceImpl storage = new S3DocumentStorageServiceImpl(s3Client, properties(null));

        assertThatThrownBy(() -> storage.open(KEY))
                .isInstanceOf(StorageException.class)
                .hasMessage("Could not read PDF from S3");
    }

    @Test
    void openWithAnSdkFailureIsStorageException() {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(SdkClientException.create("connection refused"));
        S3DocumentStorageServiceImpl storage = new S3DocumentStorageServiceImpl(s3Client, properties(null));

        assertThatThrownBy(() -> storage.open(KEY))
                .isInstanceOf(StorageException.class)
                .hasCauseInstanceOf(SdkClientException.class);
    }

    // What getObject returns: the PDF bytes as a stream, with the length S3 reported (or none)
    private static ResponseInputStream<GetObjectResponse> body(Integer length) {
        GetObjectResponse response = GetObjectResponse.builder()
                .contentLength(length == null ? null : length.longValue())
                .build();
        return new ResponseInputStream<>(response, AbortableInputStream.create(new ByteArrayInputStream(PDF)));
    }
}
