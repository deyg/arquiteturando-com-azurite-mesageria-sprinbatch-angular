package com.arquiteturando.payroll.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.kafka.listener.auto-startup=false")
class PartialFailureRecoveryTest {
    @Autowired RequestIngestionService ingestion;
    @Autowired ExportJobCoordinator coordinator;
    @Autowired JdbcClient jdbc;
    @MockitoBean PdfMotorClient pdfMotor;

    @BeforeEach
    void configureFailure() {
        when(pdfMotor.render(any())).thenAnswer(invocation -> {
            PaymentDocument item = invocation.getArgument(0);
            if (item.paymentId() == 501 || item.paymentId() == 505) {
                throw new IllegalStateException("injected PDF failure");
            }
            return item.withPdf("%PDF-valid-test".getBytes());
        });
    }

    @Test
    void finiteRetryKeepsSuccessfulItemAndMarksRequestPartial() {
        UUID id = request(List.of(101L), List.of("SALARY", "BENEFIT"));
        coordinator.launch(id);

        assertThat(requestStatus(id)).isEqualTo("PARTIAL");
        assertThat(documentStatuses(id)).containsExactlyInAnyOrder("COMPLETED", "FAILED");
        verify(pdfMotor, times(3)).render(org.mockito.ArgumentMatchers.argThat(
                item -> item.requestId().equals(id) && item.paymentId() == 501));
    }

    @Test
    void allItemsExhaustedProducesFailedRequest() {
        UUID id = request(List.of(102L), List.of("SALARY"));
        coordinator.launch(id);

        assertThat(requestStatus(id)).isEqualTo("FAILED");
        assertThat(documentStatuses(id)).containsExactly("FAILED");
    }

    private UUID request(List<Long> employees, List<String> types) {
        UUID id = UUID.randomUUID();
        ingestion.ingest(new ExportRequestedEvent(1, id, Instant.now(), employees,
                "2026-01", "2026-01", types));
        return id;
    }

    private String requestStatus(UUID id) {
        return jdbc.sql("SELECT status FROM solicitacao WHERE id=:id").param("id", id).query(String.class).single();
    }

    private List<String> documentStatuses(UUID id) {
        return jdbc.sql("SELECT status FROM documento_gerado WHERE solicitacao_id=:id ORDER BY comprovante_id")
                .param("id", id).query(String.class).list();
    }
}
