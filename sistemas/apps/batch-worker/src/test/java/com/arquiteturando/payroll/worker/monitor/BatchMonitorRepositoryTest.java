package com.arquiteturando.payroll.worker;

import static org.assertj.core.api.Assertions.assertThat;

import com.arquiteturando.payroll.worker.monitor.BatchMonitorRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.NONE,
        properties="spring.kafka.listener.auto-startup=false")
class BatchMonitorRepositoryTest {
    @Autowired BatchMonitorRepository repository;
    @Autowired RequestIngestionService ingestion;
    @Autowired ExportJobCoordinator coordinator;
    @MockitoBean PdfMotorClient pdfMotor;

    @BeforeEach void render() { when(pdfMotor.render(any())).thenAnswer(i ->
            ((PaymentDocument)i.getArgument(0)).withPdf("%PDF-monitor".getBytes())); }

    @Test void listsAndFiltersOfficialBatchMetadata() {
        UUID id=UUID.randomUUID();
        ingestion.ingest(new ExportRequestedEvent(1,id,Instant.now(),List.of(101L),"2026-01","2026-01",List.of("SALARY")));
        coordinator.launch(id);
        assertThat(repository.jobs()).anyMatch(j -> j.jobName().equals("exportPayrollDocumentsJob"));
        var page=repository.executions("exportPayrollDocumentsJob",null,id,null,null,0,10);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.items().getFirst().requestId()).isEqualTo(id);
        assertThat(page.items().getFirst().status().name()).isEqualTo("COMPLETED");
        var detail=repository.execution(page.items().getFirst().jobExecutionId());
        assertThat(detail.parameters()).containsEntry("requestId",id.toString());
        assertThat(detail.steps()).extracting(s -> s.stepName()).containsExactly(
                "prepareRequestStep","generateDocumentsStep","completeRequestStep");
        assertThat(detail.steps()).allMatch(s -> s.durationMs() != null && s.durationMs() >= 0);
        var correlation=repository.request(id);
        assertThat(correlation.requestStatus()).isEqualTo("COMPLETED");
        assertThat(correlation.executions()).hasSize(1);
    }
}
