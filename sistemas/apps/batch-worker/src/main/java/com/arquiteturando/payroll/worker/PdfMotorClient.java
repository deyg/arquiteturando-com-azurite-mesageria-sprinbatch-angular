package com.arquiteturando.payroll.worker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PdfMotorClient {
    private final RestClient client;

    public PdfMotorClient(@Value("${payroll.pdf-motor.url}") String url) {
        this.client = RestClient.builder().baseUrl(url).build();
    }

    public PaymentDocument render(PaymentDocument item) {
        var payload = new PdfRequest(item.paymentId(), item.employeeId(), item.employeeName(), item.registration(),
                item.period(), item.type(), item.description(), item.grossAmount(), item.discounts(), item.netAmount());
        byte[] pdf = client.post().uri("/internal/v1/pdf/render").contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_PDF).body(payload).retrieve().body(byte[].class);
        if (pdf == null || pdf.length < 5 || pdf[0] != '%' || pdf[1] != 'P') {
            throw new IllegalStateException("PDF motor returned invalid content");
        }
        return item.withPdf(pdf);
    }

    private record PdfRequest(long paymentId, long employeeId, String employeeName, String registration,
                              String period, String type, String description, java.math.BigDecimal grossAmount,
                              java.math.BigDecimal discounts, java.math.BigDecimal netAmount) {}
}
