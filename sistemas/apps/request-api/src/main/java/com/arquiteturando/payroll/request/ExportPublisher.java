package com.arquiteturando.payroll.request;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ExportPublisher {
    private final KafkaTemplate<String, Object> kafka;
    private final String topic;

    public ExportPublisher(KafkaTemplate<String, Object> kafka, @Value("${payroll.kafka.topic}") String topic) {
        this.kafka = kafka;
        this.topic = topic;
    }

    public AcceptedExport publish(ExportRequest request) {
        UUID requestId = UUID.randomUUID();
        var event = new ExportRequestedEvent(1, requestId, Instant.now(), request.employeeIds(),
                request.startPeriod(), request.endPeriod(), request.paymentTypes());
        try {
            kafka.send(topic, requestId.toString(), event).get(Duration.ofSeconds(10).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            return new AcceptedExport(requestId, "ACCEPTED", "/api/v1/exports/" + requestId);
        } catch (Exception exception) {
            throw new PublicationFailedException(exception);
        }
    }

    public record AcceptedExport(UUID requestId, String status, String statusUrl) {}
    public static class PublicationFailedException extends RuntimeException {
        PublicationFailedException(Throwable cause) { super("Kafka did not acknowledge the export request", cause); }
    }
}
