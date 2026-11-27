package com.joshsoftware.templateDrivenDocumentService.service.impl;

import com.joshsoftware.templateDrivenDocumentService.config.S3StorageProperties;
import com.joshsoftware.templateDrivenDocumentService.dto.StoredDocument;
import com.joshsoftware.templateDrivenDocumentService.exception.DocumentMissingException;
import com.joshsoftware.templateDrivenDocumentService.exception.StorageException;
import com.joshsoftware.templateDrivenDocumentService.service.DocumentStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

// Writes and reads PDFs in an S3 bucket: MinIO in local development, AWS S3 on staging and production.
// A PUT is atomic, so a reader never sees a half-written PDF.
@Slf4j
@Service
@RequiredArgsConstructor
public class S3DocumentStorageServiceImpl implements DocumentStorageService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final S3Client s3Client;
    private final S3StorageProperties properties;

    @Override
    public String store(String key, byte[] pdf) {
        PutObjectRequest.Builder request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .contentType(PDF_CONTENT_TYPE)
                .contentLength((long) pdf.length);
        String sse = properties.serverSideEncryption();
        if (sse != null && !sse.isBlank()) {
            request.serverSideEncryption(ServerSideEncryption.fromValue(sse));
        }
        try {
            s3Client.putObject(request.build(), RequestBody.fromBytes(pdf));
        } catch (S3Exception e) {
            // The store answered with an error: its code (NoSuchBucket, AccessDenied, SignatureDoesNotMatch, ...)
            // and HTTP status say why. Neither contains document content.
            String errorCode = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : null;
            log.error("[S3DocumentStorageServiceImpl#store] put rejected bucket={} key={} status={} s3ErrorCode={}",
                    properties.bucket(), key, e.statusCode(), errorCode);
            throw new StorageException("Could not write PDF to S3", e);
        } catch (SdkException e) {
            // No answer from the store: endpoint unreachable, timeout, or no credentials found
            log.error("[S3DocumentStorageServiceImpl#store] put failed bucket={} key={} type={}",
                    properties.bucket(), key, e.getClass().getName());
            throw new StorageException("Could not write PDF to S3", e);
        }
        log.debug("[S3DocumentStorageServiceImpl#store] stored key={} bytes={}", key, pdf.length);
        return key;
    }

    @Override
    public StoredDocument open(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(key)
                .build();
        ResponseInputStream<GetObjectResponse> body;
        try {
            body = s3Client.getObject(request);
        } catch (S3Exception e) {
            String errorCode = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : null;
            // NoSuchKey: the job is READY but its object is gone. MinIO may answer with a plain 404.
            if (e instanceof NoSuchKeyException || e.statusCode() == HttpStatus.NOT_FOUND.value()) {
                log.error("[S3DocumentStorageServiceImpl#open] get missing bucket={} key={} s3ErrorCode={}",
                        properties.bucket(), key, errorCode);
                throw new DocumentMissingException(e);
            }
            log.error("[S3DocumentStorageServiceImpl#open] get rejected bucket={} key={} status={} s3ErrorCode={}",
                    properties.bucket(), key, e.statusCode(), errorCode);
            throw new StorageException("Could not read PDF from S3", e);
        } catch (SdkException e) {
            log.error("[S3DocumentStorageServiceImpl#open] get failed bucket={} key={} type={}",
                    properties.bucket(), key, e.getClass().getName());
            throw new StorageException("Could not read PDF from S3", e);
        }
        // The body is streamed by the caller, never read into memory here
        Long length = body.response().contentLength();
        long contentLength = length == null ? -1 : length;
        log.debug("[S3DocumentStorageServiceImpl#open] opened key={} bytes={}", key, contentLength);
        return new StoredDocument(body, contentLength);
    }
}
