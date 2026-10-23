package com.arquiteturando.payroll.bff;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class BffGateway {
    private final RestClient requestApi;
    private final RestClient queryApi;

    public BffGateway(@Value("${payroll.services.request-api}") String requestUrl,
                      @Value("${payroll.services.query-api}") String queryUrl) {
        requestApi = RestClient.builder().baseUrl(requestUrl).build();
        queryApi = RestClient.builder().baseUrl(queryUrl).build();
    }

    public ResponseEntity<byte[]> request(HttpMethod method, String path, byte[] body, HttpHeaders incoming) {
        return exchange(requestApi, method, path, body, incoming);
    }

    public ResponseEntity<byte[]> query(HttpMethod method, String path, HttpHeaders incoming) {
        return exchange(queryApi, method, path, null, incoming);
    }

    private ResponseEntity<byte[]> exchange(RestClient client, HttpMethod method, String path,
                                             byte[] body, HttpHeaders incoming) {
        try {
            var request = client.method(method).uri(path).headers(headers -> copyRequestHeaders(incoming, headers));
            if (body != null) request.body(body);
            return request.retrieve().toEntity(byte[].class);
        } catch (RestClientResponseException error) {
            return ResponseEntity.status(error.getStatusCode()).headers(error.getResponseHeaders())
                    .body(error.getResponseBodyAsByteArray());
        }
    }

    private void copyRequestHeaders(HttpHeaders source, HttpHeaders target) {
        for (String name : new String[]{HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, "X-Correlation-Id"}) {
            if (source.containsHeader(name)) target.put(name, source.get(name));
        }
    }
}
