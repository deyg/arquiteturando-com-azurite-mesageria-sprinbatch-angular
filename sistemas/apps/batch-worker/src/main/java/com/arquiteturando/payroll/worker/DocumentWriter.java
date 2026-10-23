package com.arquiteturando.payroll.worker;

import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
public class DocumentWriter implements ItemWriter<PaymentDocument> {
    private final BlobStorageService storage;
    private final DocumentStateRepository documents;

    public DocumentWriter(BlobStorageService storage, DocumentStateRepository documents) {
        this.storage = storage;
        this.documents = documents;
    }

    @Override
    public void write(Chunk<? extends PaymentDocument> chunk) throws Exception {
        for (PaymentDocument item : chunk) {
            String blobName = storage.blobName(item.requestId(), item.paymentId());
            storage.upload(blobName, item.pdf());
            String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(item.pdf()));
            documents.complete(item.documentId(), blobName, checksum);
        }
    }
}
