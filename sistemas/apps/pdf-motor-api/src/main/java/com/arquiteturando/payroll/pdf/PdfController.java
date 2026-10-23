package com.arquiteturando.payroll.pdf;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/pdf")
public class PdfController {
    private final PdfRenderer renderer;

    public PdfController(PdfRenderer renderer) { this.renderer = renderer; }

    @PostMapping(value = "/render", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> render(@RequestBody PdfRenderRequest request) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("comprovante-" + request.paymentId() + ".pdf").build().toString())
                .body(renderer.render(request));
    }
}
