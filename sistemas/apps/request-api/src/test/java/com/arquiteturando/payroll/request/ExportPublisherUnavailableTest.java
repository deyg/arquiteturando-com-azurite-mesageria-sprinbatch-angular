package com.arquiteturando.payroll.request;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "spring.kafka.bootstrap-servers=127.0.0.1:19092",
            "spring.kafka.producer.properties.max.block.ms=500",
            "spring.kafka.producer.properties.request.timeout.ms=500",
            "spring.kafka.producer.properties.delivery.timeout.ms=1000"
        })
class ExportPublisherUnavailableTest {
    @Autowired ExportPublisher publisher;

    @Test
    @Timeout(5)
    void doesNotAcceptWhenBrokerIsUnavailable() {
        var request = new ExportRequest(List.of(101L), "2026-01", "2026-01", List.of("SALARY"));

        assertThatThrownBy(() -> publisher.publish(request))
                .isInstanceOf(ExportPublisher.PublicationFailedException.class)
                .hasMessageContaining("Kafka");
    }
}
