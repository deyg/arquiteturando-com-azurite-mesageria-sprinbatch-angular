package com.arquiteturando.payroll.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class ExportRequestListener {
    private final ObjectMapper mapper;
    private final RequestIngestionService ingestion;
    private final ExportJobCoordinator jobs;
    private final io.micrometer.core.instrument.Counter redeliveries;

    public ExportRequestListener(RequestIngestionService ingestion, ExportJobCoordinator jobs, MeterRegistry metrics) {
        this.ingestion = ingestion;
        this.jobs = jobs;
        this.redeliveries=metrics.counter("payroll.batch.kafka.redeliveries");
        this.mapper = new ObjectMapper().findAndRegisterModules();
    }

    @KafkaListener(topics = "${payroll.kafka.topic}")
    public void listen(String json, Acknowledgment acknowledgment) throws Exception {
        ExportRequestedEvent event = mapper.readValue(json, ExportRequestedEvent.class);
        if (event.eventVersion() != 1) throw new IllegalArgumentException("Unsupported eventVersion");
        if(!ingestion.ingest(event)) redeliveries.increment();
        jobs.launch(event.requestId());
        acknowledgment.acknowledge();
    }
}
