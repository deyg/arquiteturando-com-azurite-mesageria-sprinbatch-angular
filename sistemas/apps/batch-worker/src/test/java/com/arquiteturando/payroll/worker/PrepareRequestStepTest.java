package com.arquiteturando.payroll.worker;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.kafka.listener.auto-startup=false")
class PrepareRequestStepTest {
    @Autowired RequestIngestionService ingestion;
    @Autowired ExportJobCoordinator coordinator;
    @Autowired JdbcClient jdbc;
    @Autowired BlobStorageService storage;

    @Test
    void requestWithoutPaymentsCompletesWithZeroDocuments() {
        UUID id = ingest(List.of(101L), "2030-01", "2030-01", List.of("SALARY"));
        coordinator.launch(id);
        assertThat(status(id)).isEqualTo("COMPLETED");
        assertThat(documentCount(id)).isZero();
    }

    @Test
    void requestWithPaymentsMaterializesEachItemOnlyOnce() {
        UUID id = ingest(List.of(101L), "2026-01", "2026-02", List.of("SALARY"));
        coordinator.launch(id);
        int firstCount = documentCount(id);
        coordinator.launch(id);
        assertThat(status(id)).isEqualTo("COMPLETED");
        assertThat(firstCount).isEqualTo(2);
        assertThat(documentCount(id)).isEqualTo(2);
        var documents = jdbc.sql("SELECT blob_name FROM documento_gerado WHERE solicitacao_id=:id AND status='COMPLETED' AND length(checksum_sha256)=64")
                .param("id", id).query(String.class).list();
        assertThat(documents).hasSize(2);
        assertThat(documents).allSatisfy(blob -> {
            assertThat(storage.exists(blob)).isTrue();
            assertThat(new String(storage.download(blob), 0, 5)).isEqualTo("%PDF-");
        });
    }

    private UUID ingest(List<Long> employees, String start, String end, List<String> types) {
        UUID id = UUID.randomUUID();
        ingestion.ingest(new ExportRequestedEvent(1, id, Instant.now(), employees, start, end, types));
        return id;
    }

    private String status(UUID id) {
        return jdbc.sql("SELECT status FROM solicitacao WHERE id=:id").param("id", id).query(String.class).single();
    }

    private int documentCount(UUID id) {
        return jdbc.sql("SELECT count(*) FROM documento_gerado WHERE solicitacao_id=:id")
                .param("id", id).query(Integer.class).single();
    }
}
