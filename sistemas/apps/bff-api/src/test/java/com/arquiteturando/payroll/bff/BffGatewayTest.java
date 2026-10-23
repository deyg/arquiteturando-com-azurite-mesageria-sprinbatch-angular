package com.arquiteturando.payroll.bff;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

class BffGatewayTest {
    private HttpServer server;
    private BffGateway gateway;

    @BeforeEach
    void startUpstream() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/exports", exchange -> {
            byte[] body = "{\"requestId\":\"00000000-0000-0000-0000-000000000015\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("Location", "/api/v1/exports/00000000-0000-0000-0000-000000000015");
            exchange.getResponseHeaders().add("X-Correlation-Id", "corr-15");
            exchange.sendResponseHeaders(202, body.length); exchange.getResponseBody().write(body); exchange.close();
        });
        server.createContext("/api/v1/employees", exchange -> {
            byte[] body = "[]".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length); exchange.getResponseBody().write(body); exchange.close();
        });
        server.createContext("/api/v1/documents/doc/download", exchange -> {
            byte[] body = "%PDF-bff".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/pdf");
            exchange.sendResponseHeaders(200, body.length); exchange.getResponseBody().write(body); exchange.close();
        });
        server.start();
        String url = "http://127.0.0.1:" + server.getAddress().getPort();
        gateway = new BffGateway(url, url);
    }

    @AfterEach void stop() { server.stop(0); }

    @Test
    void preservesAcceptedStatusAndContractHeaders() {
        HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        var response = gateway.request(HttpMethod.POST, "/api/v1/exports", "{}".getBytes(), headers);
        assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(response.getHeaders().getLocation().toString()).contains("/api/v1/exports/");
        assertThat(response.getHeaders().getFirst("X-Correlation-Id")).isEqualTo("corr-15");
    }

    @Test
    void queryAndPdfUseOnlyGatewayFacade() {
        assertThat(gateway.query(HttpMethod.GET, "/api/v1/employees?startPeriod=2026-01", new HttpHeaders())
                .getStatusCode().value()).isEqualTo(200);
        var pdf = gateway.query(HttpMethod.GET, "/api/v1/documents/doc/download", new HttpHeaders());
        assertThat(pdf.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(pdf.getBody()).startsWith("%PDF-".getBytes(StandardCharsets.UTF_8));
    }
}
