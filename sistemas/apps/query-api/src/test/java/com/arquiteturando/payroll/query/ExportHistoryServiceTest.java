package com.arquiteturando.payroll.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClientBuilder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ExportHistoryServiceTest {
    @Autowired ExportHistoryService service;
    @Autowired ExportQueryController controller;
    @Autowired JdbcClient jdbc;
    private UUID requestId;
    private UUID documentId;

    @BeforeEach
    void fixture() {
        requestId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        String blob = "exports/" + requestId + "/501.pdf";
        jdbc.sql("""
                INSERT INTO solicitacao(id,status,start_period,end_period,payment_types,created_at,completed_at,
                  total_documents,completed_documents,failed_documents)
                VALUES (:id,'COMPLETED','2026-01','2026-01','SALARY',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,0)
                """).param("id", requestId).update();
        jdbc.sql("INSERT INTO solicitacao_empregado(solicitacao_id,empregado_id) VALUES (:id,101)")
                .param("id", requestId).update();
        jdbc.sql("""
                INSERT INTO documento_gerado(id,solicitacao_id,comprovante_id,status,container_name,blob_name,checksum_sha256)
                VALUES (:documentId,:requestId,501,'COMPLETED','payroll-documents',:blob,repeat('a',64))
                """).param("documentId", documentId).param("requestId", requestId).param("blob", blob).update();
        var container = new BlobContainerClientBuilder().connectionString("UseDevelopmentStorage=true")
                .containerName("payroll-documents").buildClient();
        container.createIfNotExists();
        container.getBlobClient(blob).upload(BinaryData.fromBytes("%PDF-history".getBytes(StandardCharsets.UTF_8)), true);
    }

    @Test
    void findsHistoryByEmployeePeriodWithoutRememberingRequestId() {
        var page = service.search(null, 101L, "2026-01", "2026-01", List.of("SALARY"), "COMPLETED", 0, 20);
        assertThat(page.items()).extracting(ExportHistoryService.ExportView::requestId).contains(requestId);
        assertThat(page.totalElements()).isPositive();
    }

    @Test
    void emptyHistoryReturnsContractualMessage() {
        var page = service.search(UUID.randomUUID(), null, null, null, null, null, 0, 20);
        assertThat(page.items()).isEmpty();
        assertThat(page.message()).isEqualTo(ExportHistoryService.EMPTY_MESSAGE);
    }

    @Test
    void exposesStatusDocumentsAndStreamsPdfFromAzurite() throws Exception {
        assertThat(service.get(requestId).status()).isEqualTo("COMPLETED");
        assertThat(service.documents(requestId).documents()).singleElement()
                .satisfies(document -> assertThat(document.downloadUrl()).contains(documentId.toString()));
        var response = controller.download(documentId);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/pdf");
        assertThat(response.getBody().getInputStream().readAllBytes()).startsWith("%PDF-".getBytes(StandardCharsets.UTF_8));
    }
}
