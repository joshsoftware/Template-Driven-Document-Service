package com.joshsoftware.templateDrivenDocumentService.service.impl;

import com.joshsoftware.templateDrivenDocumentService.config.crypto.PayloadCipher;
import com.joshsoftware.templateDrivenDocumentService.config.crypto.PayloadEncryptionProperties;
import com.joshsoftware.templateDrivenDocumentService.exception.JobPayloadException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Real Postgres (Flyway-migrated); every test runs in a transaction that is rolled back
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({DbJobPayloadStore.class, DbJobPayloadStoreTest.TestBeans.class})
class DbJobPayloadStoreTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final JsonNode MAPPED = JSON.readTree("""
            {"customerName":"TEST CUSTOMER","accountNumber":"10000000000001","transactions":[{"amount":14400}]}
            """);

    @Autowired
    private DbJobPayloadStore store;

    @Autowired
    private EntityManager entityManager;

    @Test
    void loadReturnsWhatWasSaved() {
        UUID jobId = job("PROCESSING");

        store.save(jobId, MAPPED);

        assertThat(store.load(jobId)).isEqualTo(MAPPED);
    }

    @Test
    void tableHoldsOnlyCiphertextAndTheKeyId() {
        UUID jobId = job("PROCESSING");

        store.save(jobId, MAPPED);

        Object[] row = (Object[]) entityManager
                .createNativeQuery("SELECT payload_encrypted, key_id FROM job_queue WHERE job_id = :id")
                .setParameter("id", jobId).getSingleResult();
        String stored = new String((byte[]) row[0], StandardCharsets.ISO_8859_1);
        assertThat(stored).doesNotContain("TEST CUSTOMER", "10000000000001");
        assertThat(row[1]).isEqualTo("v1");
    }

    @Test
    void clearDeletesTheRowAndCanRunTwice() {
        UUID jobId = job("PROCESSING");
        store.save(jobId, MAPPED);

        store.clear(jobId);

        assertThatThrownBy(() -> store.load(jobId))
                .isInstanceOf(JobPayloadException.class)
                .hasMessage("No queued payload: jobId=" + jobId);
        assertThatCode(() -> store.clear(jobId)).doesNotThrowAnyException();
    }

    @Test
    void saveIsRefusedForAFinishedJob() {
        UUID jobId = job("READY");

        assertThatThrownBy(() -> store.save(jobId, MAPPED))
                .isInstanceOf(JobPayloadException.class)
                .hasMessage("Job is not waiting to be rendered: jobId=" + jobId);
    }

    @Test
    void saveIsRefusedForAnUnknownJob() {
        assertThatThrownBy(() -> store.save(UUID.randomUUID(), MAPPED)).isInstanceOf(JobPayloadException.class);
    }

    @Test
    void secondSaveForTheSameJobFails() {
        UUID jobId = job("PROCESSING");
        store.save(jobId, MAPPED);

        assertThatThrownBy(() -> store.save(jobId, MAPPED)).isInstanceOf(RuntimeException.class);
    }

    // A disabled template version of its own, so the real ACTIVE templates are never touched
    private UUID job(String status) {
        UUID templateVersionId = UUID.randomUUID();
        entityManager.createNativeQuery("""
                        INSERT INTO templates (id, template_id, name, version, status, template_html, contract_json, mapping_json)
                        VALUES (:id, :templateId, 'payload store test', 1, 'DISABLED', '<html/>', '{}', '{}')
                        """)
                .setParameter("id", templateVersionId)
                .setParameter("templateId", "test-" + templateVersionId)
                .executeUpdate();
        UUID jobId = UUID.randomUUID();
        entityManager.createNativeQuery("""
                        INSERT INTO document_jobs (id, template_version_id, channel, status)
                        VALUES (:id, :templateVersionId, 'MOBILE', CAST(:status AS job_status))
                        """)
                .setParameter("id", jobId)
                .setParameter("templateVersionId", templateVersionId)
                .setParameter("status", status)
                .executeUpdate();
        return jobId;
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        PayloadCipher payloadCipher() {
            byte[] key = new byte[32];
            Arrays.fill(key, (byte) 7);
            return new PayloadCipher(new PayloadEncryptionProperties("v1",
                    Map.of("v1", Base64.getEncoder().encodeToString(key))));
        }

        @Bean
        JsonMapper jsonMapper() {
            return JSON;
        }
    }
}
