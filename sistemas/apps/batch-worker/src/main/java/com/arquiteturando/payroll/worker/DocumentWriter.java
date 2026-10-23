package com.arquiteturando.payroll.worker;

import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class DocumentWriter implements ItemWriter<PaymentDocument> {
    private final BlobStorageService storage;
    private final DocumentStateRepository documents;
    private final Counter sqlFailuresAfterUpload;

    public DocumentWriter(BlobStorageService storage, DocumentStateRepository documents, MeterRegistry metrics) {
        this.storage = storage;
        this.documents = documents;
        this.sqlFailuresAfterUpload=metrics.counter("payroll.batch.sql.failures.after.upload");
    }

    @Override
    public void write(Chunk<? extends PaymentDocument> chunk) throws Exception {
        for (PaymentDocument item : chunk) {
            String blobName = storage.blobName(item.requestId(), item.paymentId());
            storage.upload(blobName, item.pdf());
            String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(item.pdf()));
            try { documents.complete(item.documentId(), blobName, checksum); }
            catch(RuntimeException exception){ sqlFailuresAfterUpload.increment(); throw exception; }
        }
    }
}
