package com.arquiteturando.payroll.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

@Service
public class PdfRenderer {
    public byte[] render(PdfRenderRequest request) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.setLeading(18);
                content.newLineAtOffset(60, 740);
                line(content, "Comprovante de pagamento");
                line(content, "Empregado: " + request.employeeName());
                line(content, "Matricula: " + request.registration());
                line(content, "Competencia: " + request.period());
                line(content, "Tipo: " + request.type());
                line(content, "Descricao: " + request.description());
                line(content, "Bruto: " + request.grossAmount());
                line(content, "Descontos: " + request.discounts());
                line(content, "Liquido: " + request.netAmount());
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not render PDF", exception);
        }
    }

    private static void line(PDPageContentStream content, String text) throws IOException {
        content.showText(text);
        content.newLine();
    }
}
