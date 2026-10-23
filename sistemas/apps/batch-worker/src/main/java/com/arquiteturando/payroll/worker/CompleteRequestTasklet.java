package com.arquiteturando.payroll.worker;

import java.util.UUID;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class CompleteRequestTasklet implements Tasklet {
    private final JdbcClient jdbc;
    public CompleteRequestTasklet(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override
    public RepeatStatus execute(org.springframework.batch.core.step.StepContribution contribution,
                                org.springframework.batch.core.scope.context.ChunkContext context) {
        UUID id = UUID.fromString(context.getStepContext().getJobParameters().get("requestId").toString());
        jdbc.sql("""
                UPDATE solicitacao s SET status='COMPLETED',completed_at=CURRENT_TIMESTAMP,
                  completed_documents=(SELECT count(*) FROM documento_gerado d WHERE d.solicitacao_id=s.id AND d.status='COMPLETED'),
                  failed_documents=(SELECT count(*) FROM documento_gerado d WHERE d.solicitacao_id=s.id AND d.status='FAILED')
                WHERE s.id=:id
                """).param("id", id).update();
        return RepeatStatus.FINISHED;
    }
}
