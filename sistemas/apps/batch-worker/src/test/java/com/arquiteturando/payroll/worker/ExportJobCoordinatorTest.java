package com.arquiteturando.payroll.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.kafka.listener.auto-startup=false"
})
class ExportJobCoordinatorTest {
    @Autowired RequestIngestionService ingestion;
    @Autowired ExportJobCoordinator coordinator;
    @Autowired JobExplorer explorer;
    @MockitoBean PdfMotorClient pdfMotor;

    @BeforeEach
    void renderPdf() {
        when(pdfMotor.render(any())).thenAnswer(invocation ->
                ((PaymentDocument) invocation.getArgument(0)).withPdf("%PDF-test".getBytes()));
    }

    @Test
    void stableRequestIdCreatesOnlyOneCompletedJobInstance() {
        UUID requestId = UUID.randomUUID();
        ingestion.ingest(new ExportRequestedEvent(1, requestId, Instant.now(), List.of(101L),
                "2026-01", "2026-01", List.of("SALARY")));

        coordinator.launch(requestId);
        coordinator.launch(requestId);

        var parameters = new JobParametersBuilder()
                .addString("requestId", requestId.toString(), true).toJobParameters();
        var instance = explorer.getJobInstance("exportPayrollDocumentsJob", parameters);
        assertThat(instance).isNotNull();
        assertThat(explorer.getJobExecutions(instance)).hasSize(1);
        assertThat(explorer.getJobExecutions(instance).getFirst().getStatus().name()).isEqualTo("COMPLETED");
    }
}
