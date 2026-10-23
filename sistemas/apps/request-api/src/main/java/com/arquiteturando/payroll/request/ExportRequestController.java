package com.arquiteturando.payroll.request;

import java.net.URI;
import java.time.YearMonth;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@RestController
@RequestMapping("/api/v1/exports")
public class ExportRequestController {
    private final ExportPublisher publisher;
    public ExportRequestController(ExportPublisher publisher) { this.publisher = publisher; }

    @PostMapping
    public ResponseEntity<ExportPublisher.AcceptedExport> request(@RequestBody ExportRequest request) {
        validate(request);
        var accepted = publisher.publish(request);
        return ResponseEntity.accepted().location(URI.create(accepted.statusUrl())).body(accepted);
    }

    @ExceptionHandler(ExportPublisher.PublicationFailedException.class)
    ResponseEntity<Void> publicationFailed() { return ResponseEntity.status(SERVICE_UNAVAILABLE).build(); }

    private static void validate(ExportRequest request) {
        if (request.employeeIds() == null || request.employeeIds().isEmpty()) throw new ResponseStatusException(BAD_REQUEST, "employeeIds is required");
        if (request.paymentTypes() == null || request.paymentTypes().isEmpty() || request.paymentTypes().stream().anyMatch(t -> !t.equals("SALARY") && !t.equals("BENEFIT"))) throw new ResponseStatusException(BAD_REQUEST, "invalid paymentTypes");
        try { if (YearMonth.parse(request.startPeriod()).isAfter(YearMonth.parse(request.endPeriod()))) throw new ResponseStatusException(BAD_REQUEST, "invalid period"); }
        catch (RuntimeException exception) { if (exception instanceof ResponseStatusException response) throw response; throw new ResponseStatusException(BAD_REQUEST, "periods must use YYYY-MM", exception); }
    }
}
