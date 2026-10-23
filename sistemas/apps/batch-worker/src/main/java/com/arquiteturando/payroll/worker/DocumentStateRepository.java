package com.arquiteturando.payroll.worker;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Repository
public class DocumentStateRepository {
    private final JdbcClient jdbc;
    private final String container;
    private final Timer duration;

    public DocumentStateRepository(JdbcClient jdbc, @Value("${payroll.storage.container}") String container,
                                   MeterRegistry metrics) {
        this.jdbc = jdbc;
        this.container = container;
        this.duration=metrics.timer("payroll.batch.sql.duration","operation","complete_document");
    }

    public void complete(UUID documentId, String blobName, String checksum) {
        int updated = duration.record(() -> jdbc.sql("""
                UPDATE documento_gerado SET status='COMPLETED',container_name=:container,
                  blob_name=:blob,checksum_sha256=:checksum,error_message=NULL,updated_at=CURRENT_TIMESTAMP
                WHERE id=:id
                """).param("container", container).param("blob", blobName)
                .param("checksum", checksum).param("id", documentId).update());
        if (updated != 1) {
            throw new IllegalStateException("Document state was not persisted: " + documentId);
        }
    }
}
