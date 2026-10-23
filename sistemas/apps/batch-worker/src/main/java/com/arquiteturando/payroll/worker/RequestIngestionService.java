package com.arquiteturando.payroll.worker;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestIngestionService {
    private final JdbcClient jdbc;
    public RequestIngestionService(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Transactional
    public void ingest(ExportRequestedEvent event) {
        jdbc.sql("""
                INSERT INTO solicitacao(id,status,start_period,end_period,payment_types,created_at)
                VALUES (:id,'PENDING',:start,:end,:types,:created)
                ON CONFLICT (id) DO NOTHING
                """).param("id", event.requestId()).param("start", event.startPeriod()).param("end", event.endPeriod())
                .param("types", String.join(",", event.paymentTypes()))
                .param("created", event.createdAt().atOffset(java.time.ZoneOffset.UTC)).update();
        for (Long employeeId : event.employeeIds()) {
            jdbc.sql("""
                    INSERT INTO solicitacao_empregado(solicitacao_id,empregado_id)
                    VALUES (:requestId,:employeeId) ON CONFLICT DO NOTHING
                    """).param("requestId", event.requestId()).param("employeeId", employeeId).update();
        }
    }
}
