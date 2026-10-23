package com.arquiteturando.payroll.query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ExportHistoryService {
    public static final String EMPTY_MESSAGE = "Nenhuma solicitação encontrada para os filtros informados.";
    private final JdbcClient jdbc;

    public ExportHistoryService(JdbcClient jdbc) { this.jdbc = jdbc; }

    public HistoryPage search(UUID requestId, Long employeeId, String startPeriod, String endPeriod,
                              List<String> paymentTypes, String status, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (requestId != null) where.append(" AND s.id=:requestId");
        if (employeeId != null) where.append(" AND EXISTS (SELECT 1 FROM solicitacao_empregado se WHERE se.solicitacao_id=s.id AND se.empregado_id=:employeeId)");
        if (startPeriod != null) where.append(" AND s.start_period>=:startPeriod");
        if (endPeriod != null) where.append(" AND s.end_period<=:endPeriod");
        if (paymentTypes != null && !paymentTypes.isEmpty()) where.append(" AND string_to_array(s.payment_types, ',') && string_to_array(:paymentTypes, ',')");
        if (status != null) where.append(" AND s.status=:status");
        String filter = where.toString();
        JdbcClient.StatementSpec count = bind(jdbc.sql("SELECT count(*) FROM solicitacao s" + filter), requestId, employeeId, startPeriod, endPeriod, paymentTypes, status);
        long total = count.query(Long.class).single();
        JdbcClient.StatementSpec query = bind(jdbc.sql("""
                SELECT s.id,s.status,s.start_period,s.end_period,s.payment_types,s.created_at,
                       s.total_documents,s.completed_documents,s.failed_documents,s.error_message
                FROM solicitacao s
                """ + filter + " ORDER BY s.created_at DESC,s.id DESC LIMIT :size OFFSET :offset"),
                requestId, employeeId, startPeriod, endPeriod, paymentTypes, status).param("size", size).param("offset", page * size);
        List<ExportView> items = query.query((rs, row) -> mapExport(rs)).list();
        return new HistoryPage(items, page, size, total, (total + size - 1) / size,
                items.isEmpty() ? EMPTY_MESSAGE : null);
    }

    public ExportView get(UUID requestId) {
        return jdbc.sql("""
                SELECT id,status,start_period,end_period,payment_types,created_at,total_documents,
                       completed_documents,failed_documents,error_message FROM solicitacao WHERE id=:id
                """).param("id", requestId).query((rs, row) -> mapExport(rs)).optional()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Solicitação não encontrada"));
    }

    public DocumentsView documents(UUID requestId) {
        get(requestId);
        var documents = jdbc.sql("""
                SELECT d.id,d.comprovante_id,p.empregado_id,d.status,d.error_message
                FROM documento_gerado d JOIN comprovante_pagamento p ON p.id=d.comprovante_id
                WHERE d.solicitacao_id=:id ORDER BY d.comprovante_id
                """).param("id", requestId).query((rs, row) -> new DocumentView(
                rs.getObject(1, UUID.class), rs.getLong(2), rs.getLong(3), rs.getString(4),
                "comprovante-" + rs.getLong(2) + ".pdf",
                rs.getString(4).equals("COMPLETED") ? "/api/v1/documents/" + rs.getObject(1) + "/download" : null,
                rs.getString(5))).list();
        return new DocumentsView(requestId, documents);
    }

    public String completedBlob(UUID documentId) {
        return jdbc.sql("SELECT blob_name FROM documento_gerado WHERE id=:id AND status='COMPLETED'")
                .param("id", documentId).query(String.class).optional()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Documento concluído não encontrado"));
    }

    private JdbcClient.StatementSpec bind(JdbcClient.StatementSpec spec, UUID requestId, Long employeeId,
                                           String start, String end, List<String> paymentTypes, String status) {
        if (requestId != null) spec = spec.param("requestId", requestId);
        if (employeeId != null) spec = spec.param("employeeId", employeeId);
        if (start != null) spec = spec.param("startPeriod", start);
        if (end != null) spec = spec.param("endPeriod", end);
        if (paymentTypes != null && !paymentTypes.isEmpty()) spec = spec.param("paymentTypes", String.join(",", paymentTypes));
        if (status != null) spec = spec.param("status", status);
        return spec;
    }

    private ExportView mapExport(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ExportView(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getString(4),
                List.of(rs.getString(5).split(",")), rs.getObject(6, OffsetDateTime.class),
                rs.getInt(7), rs.getInt(8), rs.getInt(9), rs.getString(10));
    }

    public record ExportView(UUID requestId, String status, String startPeriod, String endPeriod,
                             List<String> paymentTypes, OffsetDateTime createdAt, int totalDocuments,
                             int completedDocuments, int failedDocuments, String errorMessage) {}
    public record HistoryPage(List<ExportView> items, int page, int size, long totalElements,
                              long totalPages, String message) {}
    public record DocumentView(UUID documentId, long paymentId, long employeeId, String status,
                               String fileName, String downloadUrl, String errorMessage) {}
    public record DocumentsView(UUID requestId, List<DocumentView> documents) {}
}
