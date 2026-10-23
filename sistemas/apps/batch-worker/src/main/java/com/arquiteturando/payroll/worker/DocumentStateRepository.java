package com.arquiteturando.payroll.worker;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class DocumentStateRepository {
    private final JdbcClient jdbc;
    private final String container;

    public DocumentStateRepository(JdbcClient jdbc, @Value("${payroll.storage.container}") String container) {
        this.jdbc = jdbc;
        this.container = container;
    }

    public void complete(UUID documentId, String blobName, String checksum) {
        int updated = jdbc.sql("""
                UPDATE documento_gerado SET status='COMPLETED',container_name=:container,
                  blob_name=:blob,checksum_sha256=:checksum,error_message=NULL,updated_at=CURRENT_TIMESTAMP
                WHERE id=:id
                """).param("container", container).param("blob", blobName)
                .param("checksum", checksum).param("id", documentId).update();
        if (updated != 1) {
            throw new IllegalStateException("Document state was not persisted: " + documentId);
        }
    }
}
