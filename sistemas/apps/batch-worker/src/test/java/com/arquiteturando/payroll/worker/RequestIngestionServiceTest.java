package com.arquiteturando.payroll.worker;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.kafka.listener.auto-startup=false")
class RequestIngestionServiceTest {
    @Autowired RequestIngestionService ingestion;
    @Autowired JdbcClient jdbc;

    @Test
    void duplicateDeliveryCreatesOneRequestAndUniqueLinks() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000009");
        var event = new ExportRequestedEvent(1, id, Instant.parse("2026-10-23T17:00:00Z"),
                List.of(101L, 102L), "2026-01", "2026-02", List.of("SALARY"));
        ingestion.ingest(event);
        ingestion.ingest(event);
        Integer requests = jdbc.sql("SELECT count(*) FROM solicitacao WHERE id=:id").param("id", id).query(Integer.class).single();
        Integer links = jdbc.sql("SELECT count(*) FROM solicitacao_empregado WHERE solicitacao_id=:id").param("id", id).query(Integer.class).single();
        assertThat(requests).isEqualTo(1);
        assertThat(links).isEqualTo(2);
    }
}
