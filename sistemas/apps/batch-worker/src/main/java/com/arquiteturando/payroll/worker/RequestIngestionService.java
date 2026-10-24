package com.arquiteturando.payroll.worker;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestIngestionService {
    private final JdbcClient jdbc;
    public RequestIngestionService(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Transactional
    public boolean ingest(ExportRequestedEvent event) {
        int inserted=jdbc.sql("""
                INSERT IGNORE INTO solicitacao(id,status,start_period,end_period,payment_types,created_at)
                VALUES (:id,'PENDING',:start,:end,:types,:created)
                """).param("id", event.requestId().toString()).param("start", event.startPeriod()).param("end", event.endPeriod())
                .param("types", String.join(",", event.paymentTypes()))
                .param("created", event.createdAt().atOffset(java.time.ZoneOffset.UTC)).update();
        for (Long employeeId : event.employeeIds()) {
            jdbc.sql("""
                    INSERT IGNORE INTO solicitacao_empregado(solicitacao_id,empregado_id)
                    VALUES (:requestId,:employeeId)
                    """).param("requestId", event.requestId().toString()).param("employeeId", employeeId).update();
        }
        return inserted == 1;
    }
}
