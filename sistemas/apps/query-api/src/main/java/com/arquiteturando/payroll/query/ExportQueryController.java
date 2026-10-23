package com.arquiteturando.payroll.query;

import java.util.UUID;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/v1")
public class ExportQueryController {
    private final ExportHistoryService history;
    private final QueryBlobStorage storage;
    public ExportQueryController(ExportHistoryService history, QueryBlobStorage storage) {
        this.history = history; this.storage = storage;
    }

    @GetMapping("/exports")
    ExportHistoryService.HistoryPage search(@RequestParam(required=false) UUID requestId,
            @RequestParam(required=false) Long employeeId,
            @RequestParam(required=false) String startPeriod, @RequestParam(required=false) String endPeriod,
            @RequestParam(required=false) List<String> paymentTypes,
            @RequestParam(required=false) String status, @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(BAD_REQUEST, "Paginação inválida");
        return history.search(requestId, employeeId, startPeriod, endPeriod, paymentTypes, status, page, size);
    }

    @GetMapping("/exports/{requestId}")
    ExportHistoryService.ExportView get(@PathVariable UUID requestId) { return history.get(requestId); }

    @GetMapping("/exports/{requestId}/documents")
    ExportHistoryService.DocumentsView documents(@PathVariable UUID requestId) { return history.documents(requestId); }

    @GetMapping(value="/documents/{documentId}/download", produces=MediaType.APPLICATION_PDF_VALUE)
    ResponseEntity<InputStreamResource> download(@PathVariable UUID documentId) {
        String blob = history.completedBlob(documentId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(blob.substring(blob.lastIndexOf('/') + 1)).build().toString())
                .body(new InputStreamResource(storage.open(blob)));
    }
}
