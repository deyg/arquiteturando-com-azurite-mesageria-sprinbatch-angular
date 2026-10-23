package com.arquiteturando.payroll.worker;

import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class DocumentWriter implements ItemWriter<PaymentDocument> {
    private final BlobStorageService storage;
    private final JdbcClient jdbc;
    private final String container;

    public DocumentWriter(BlobStorageService storage, JdbcClient jdbc,
                          @Value("${payroll.storage.container}") String container) {
        this.storage = storage;
        this.jdbc = jdbc;
        this.container = container;
    }

    @Override
    public void write(Chunk<? extends PaymentDocument> chunk) throws Exception {
        for (PaymentDocument item : chunk) {
            String blobName = storage.blobName(item.requestId(), item.paymentId());
            storage.upload(blobName, item.pdf());
            String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(item.pdf()));
            jdbc.sql("""
                    UPDATE documento_gerado SET status='COMPLETED',container_name=:container,
                      blob_name=:blob,checksum_sha256=:checksum,error_message=NULL,updated_at=CURRENT_TIMESTAMP
                    WHERE id=:id
                    """).param("container", container).param("blob", blobName)
                    .param("checksum", checksum).param("id", item.documentId()).update();
        }
    }
}
