package com.joshsoftware.templateDrivenDocumentService.service.impl;

import com.joshsoftware.templateDrivenDocumentService.config.crypto.EncryptedPayload;
import com.joshsoftware.templateDrivenDocumentService.config.crypto.PayloadCipher;
import com.joshsoftware.templateDrivenDocumentService.entity.JobQueueEntry;
import com.joshsoftware.templateDrivenDocumentService.exception.JobPayloadException;
import com.joshsoftware.templateDrivenDocumentService.repository.JobQueueRepository;
import com.joshsoftware.templateDrivenDocumentService.service.JobPayloadStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

// Encrypted payloads in job_queue. Joins the caller's transaction (or opens one), so markReady / markFailed
// can clear in the same transaction as the status change. Logs show job id, key id and sizes only.
@Slf4j
@Service
@RequiredArgsConstructor
public class DbJobPayloadStore implements JobPayloadStore {

    private final JobQueueRepository jobQueueRepository;
    private final PayloadCipher payloadCipher;
    private final JsonMapper jsonMapper;

    @Override
    @Transactional
    public void save(UUID jobId, JsonNode mapped) {
        EncryptedPayload sealed = payloadCipher.encrypt(jobId, jsonMapper.writeValueAsBytes(mapped));
        if (jobQueueRepository.insertIfProcessing(jobId, sealed.data(), sealed.keyId()) != 1) {
            throw new JobPayloadException("Job is not waiting to be rendered", jobId);
        }
        log.debug("[DbJobPayloadStore#save] queued jobId={} keyId={} bytes={}", jobId, sealed.keyId(), sealed.data().length);
    }

    @Override
    @Transactional(readOnly = true)
    public JsonNode load(UUID jobId) {
        JobQueueEntry entry = jobQueueRepository.findById(jobId)
                .orElseThrow(() -> new JobPayloadException("No queued payload", jobId));
        byte[] json = payloadCipher.decrypt(jobId, new EncryptedPayload(entry.getKeyId(), entry.getPayloadEncrypted()));
        return jsonMapper.readTree(json);
    }

    @Override
    @Transactional
    public void clear(UUID jobId) {
        int deleted = jobQueueRepository.deleteByJobId(jobId);
        log.debug("[DbJobPayloadStore#clear] jobId={} deleted={}", jobId, deleted);
    }
}
