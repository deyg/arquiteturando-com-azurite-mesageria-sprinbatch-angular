package com.arquiteturando.payroll.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class ExportRequestListener {
    private final ObjectMapper mapper;
    private final RequestIngestionService ingestion;
    private final ExportJobCoordinator jobs;

    public ExportRequestListener(RequestIngestionService ingestion, ExportJobCoordinator jobs) {
        this.ingestion = ingestion;
        this.jobs = jobs;
        this.mapper = new ObjectMapper().findAndRegisterModules();
    }

    @KafkaListener(topics = "${payroll.kafka.topic}")
    public void listen(String json, Acknowledgment acknowledgment) throws Exception {
        ExportRequestedEvent event = mapper.readValue(json, ExportRequestedEvent.class);
        if (event.eventVersion() != 1) throw new IllegalArgumentException("Unsupported eventVersion");
        ingestion.ingest(event);
        jobs.launch(event.requestId());
        acknowledgment.acknowledge();
    }
}
