package com.arquiteturando.payroll.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class PdfRendererTest {
    @Test
    void rendersValidPdfWithPaymentContent() throws Exception {
        var request = new PdfRenderRequest(501, 101, "Ana Horizonte", "MAT-101", "2026-01", "SALARY",
                "Salário mensal", new BigDecimal("6500.00"), new BigDecimal("1250.00"), new BigDecimal("5250.00"));
        byte[] pdf = new PdfRenderer().render(request);
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        try (var document = Loader.loadPDF(pdf)) {
            assertThat(new PDFTextStripper().getText(document))
                    .contains("Ana Horizonte", "MAT-101", "2026-01", "5250.00");
        }
    }
}
