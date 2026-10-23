package com.arquiteturando.payroll.worker;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;

@ExtendWith(MockitoExtension.class)
class DocumentWriterRecoveryTest {
    @Mock BlobStorageService storage;
    @Mock DocumentStateRepository documents;

    @Test
    void retryOverwritesSameBlobAfterSqlFailureAndOnlyThenCompletesState() throws Exception {
        UUID documentId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        byte[] pdf = "%PDF-retry".getBytes();
        var item = new PaymentDocument(documentId, requestId, 501, 101, "Ana", "E-101", "2026-01",
                "SALARY", "Salário", BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, pdf);
        String blobName = requestId + "/501.pdf";
        when(storage.blobName(requestId, 501)).thenReturn(blobName);
        doThrow(new IllegalStateException("injected SQL failure"))
                .doNothing().when(documents).complete(org.mockito.ArgumentMatchers.eq(documentId),
                        org.mockito.ArgumentMatchers.eq(blobName), org.mockito.ArgumentMatchers.anyString());
        var writer = new DocumentWriter(storage, documents);

        assertThatThrownBy(() -> writer.write(new Chunk<>(item)))
                .hasMessageContaining("injected SQL failure");
        writer.write(new Chunk<>(item));

        verify(storage, times(2)).upload(blobName, pdf);
        verify(documents, times(2)).complete(org.mockito.ArgumentMatchers.eq(documentId),
                org.mockito.ArgumentMatchers.eq(blobName), org.mockito.ArgumentMatchers.anyString());
    }
}
