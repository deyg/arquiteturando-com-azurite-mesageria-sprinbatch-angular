package com.arquiteturando.payroll.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ExportPublisherTest {
    @Autowired ExportPublisher publisher;

    @Test
    void publishesRealVersionedEventAndWaitsForAck() {
        var accepted = publisher.publish(new ExportRequest(List.of(101L), "2026-01", "2026-02", List.of("SALARY")));
        assertThat(accepted.status()).isEqualTo("ACCEPTED");
        var properties = java.util.Map.<String, Object>of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092",
                ConsumerConfig.GROUP_ID_CONFIG, "request-api-test-" + accepted.requestId(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        var factory = new DefaultKafkaConsumerFactory<String, String>(properties, new StringDeserializer(), new StringDeserializer());
        try (var consumer = factory.createConsumer()) {
            consumer.subscribe(List.of("payroll.exports.requested.v1"));
            var records = consumer.poll(java.time.Duration.ofSeconds(5));
            assertThat(records).anyMatch(record -> record.key().equals(accepted.requestId().toString()) && record.value().contains("\"eventVersion\":1"));
        }
    }
}
