package com.arquiteturando.payroll.worker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class PdfMotorClient {
    private final RestClient client;
    private final Timer duration;
    private final io.micrometer.core.instrument.Counter failures;
    private final io.micrometer.core.instrument.Counter retries;

    public PdfMotorClient(@Value("${payroll.pdf-motor.url}") String url, MeterRegistry metrics) {
        this.client = RestClient.builder().baseUrl(url).build();
        this.duration=Timer.builder("payroll.batch.pdf.duration").publishPercentiles(.5,.95,.99).publishPercentileHistogram().register(metrics);
        this.failures=metrics.counter("payroll.batch.pdf.failures");
        this.retries=metrics.counter("payroll.batch.pdf.retries");
    }

    public PaymentDocument render(PaymentDocument item) {
        var payload = new PdfRequest(item.paymentId(), item.employeeId(), item.employeeName(), item.registration(),
                item.period(), item.type(), item.description(), item.grossAmount(), item.discounts(), item.netAmount());
        try {
            byte[] pdf = duration.recordCallable(() -> client.post().uri("/internal/v1/pdf/render")
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_PDF).body(payload)
                    .retrieve().body(byte[].class));
            if (pdf == null || pdf.length < 5 || pdf[0] != '%' || pdf[1] != 'P') {
                throw new IllegalStateException("PDF motor returned invalid content");
            }
            return item.withPdf(pdf);
        } catch (RuntimeException exception) {
            failures.increment(); retries.increment(); throw exception;
        } catch (Exception exception) {
            failures.increment(); retries.increment(); throw new IllegalStateException(exception);
        }
    }

    private record PdfRequest(long paymentId, long employeeId, String employeeName, String registration,
                              String period, String type, String description, java.math.BigDecimal grossAmount,
                              java.math.BigDecimal discounts, java.math.BigDecimal netAmount) {}
}
