package com.arquiteturando.payroll.worker;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class PrepareRequestTasklet implements Tasklet {
    private final JdbcClient jdbc;

    public PrepareRequestTasklet(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public RepeatStatus execute(org.springframework.batch.core.step.StepContribution contribution,
                                org.springframework.batch.core.scope.context.ChunkContext context) {
        UUID requestId = UUID.fromString(context.getStepContext().getJobParameters().get("requestId").toString());
        var paymentIds = jdbc.sql("""
                SELECT p.id FROM comprovante_pagamento p
                JOIN solicitacao_empregado se ON se.empregado_id=p.empregado_id
                JOIN solicitacao s ON s.id=se.solicitacao_id
                WHERE s.id=:requestId AND p.competencia BETWEEN s.start_period AND s.end_period
                  AND FIND_IN_SET(p.tipo, s.payment_types) > 0
                ORDER BY p.id
                """).param("requestId", requestId.toString()).query(Long.class).list();

        for (Long paymentId : paymentIds) {
            UUID documentId = UUID.nameUUIDFromBytes((requestId + ":" + paymentId).getBytes(StandardCharsets.UTF_8));
            jdbc.sql("""
                    INSERT IGNORE INTO documento_gerado(id,solicitacao_id,comprovante_id,status)
                    VALUES (:id,:requestId,:paymentId,'PENDING')
                    """).param("id", documentId.toString()).param("requestId", requestId.toString())
                    .param("paymentId", paymentId).update();
        }
        String status = paymentIds.isEmpty() ? "COMPLETED" : "PROCESSING";
        jdbc.sql("""
                UPDATE solicitacao SET status=:status, started_at=COALESCE(started_at,CURRENT_TIMESTAMP),
                    completed_at=CASE WHEN :status='COMPLETED' THEN CURRENT_TIMESTAMP ELSE NULL END,
                    total_documents=(SELECT count(*) FROM documento_gerado WHERE solicitacao_id=:requestId)
                WHERE id=:requestId
                """).param("status", status).param("requestId", requestId.toString()).update();
        return RepeatStatus.FINISHED;
    }
}
