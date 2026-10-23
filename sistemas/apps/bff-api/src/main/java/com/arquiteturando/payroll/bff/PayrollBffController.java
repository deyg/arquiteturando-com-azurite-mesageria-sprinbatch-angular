package com.arquiteturando.payroll.bff;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PayrollBffController {
    private final BffGateway gateway;
    public PayrollBffController(BffGateway gateway) { this.gateway = gateway; }

    @GetMapping({"/employees", "/employees/{employeeId}/payments"})
    ResponseEntity<byte[]> employees(HttpServletRequest request, @RequestHeader HttpHeaders headers,
                                     @PathVariable(required=false) Long employeeId) {
        return gateway.query(HttpMethod.GET, relative(request), headers);
    }

    @PostMapping("/exports")
    ResponseEntity<byte[]> requestExport(HttpServletRequest request, @RequestHeader HttpHeaders headers,
                                         @RequestBody byte[] body) {
        return gateway.request(HttpMethod.POST, relative(request), body, headers);
    }

    @GetMapping({"/exports", "/exports/{requestId}", "/exports/{requestId}/documents",
                 "/documents/{documentId}/download"})
    ResponseEntity<byte[]> exports(HttpServletRequest request, @RequestHeader HttpHeaders headers,
                                   @PathVariable(required=false) String requestId,
                                   @PathVariable(required=false) String documentId) {
        return gateway.query(HttpMethod.GET, relative(request), headers);
    }

    private String relative(HttpServletRequest request) {
        return request.getRequestURI() + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
    }
}
