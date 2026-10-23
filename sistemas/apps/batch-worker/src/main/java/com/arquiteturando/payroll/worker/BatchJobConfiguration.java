package com.arquiteturando.payroll.worker;

import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import javax.sql.DataSource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchJobConfiguration {
    @Bean
    Step prepareRequestStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                            PrepareRequestTasklet tasklet) {
        return new StepBuilder("prepareRequestStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    Job exportPayrollDocumentsJob(JobRepository jobRepository, Step prepareRequestStep, Step generateDocumentsStep,
                                  Step completeRequestStep) {
        return new JobBuilder("exportPayrollDocumentsJob", jobRepository)
                .start(prepareRequestStep)
                .next(generateDocumentsStep)
                .next(completeRequestStep)
                .build();
    }

    @Bean
    @StepScope
    JdbcCursorItemReader<PaymentDocument> pendingDocumentReader(DataSource dataSource,
            @Value("#{jobParameters['requestId']}") String requestId) {
        return new JdbcCursorItemReaderBuilder<PaymentDocument>().name("pendingDocumentReader")
                .dataSource(dataSource).sql("""
                    SELECT d.id,d.solicitacao_id,p.id,e.id,e.nome_ficticio,e.matricula,p.competencia,p.tipo,
                           p.descricao,p.valor_bruto,p.descontos,p.valor_liquido
                    FROM documento_gerado d JOIN comprovante_pagamento p ON p.id=d.comprovante_id
                    JOIN empregado e ON e.id=p.empregado_id
                    WHERE d.solicitacao_id=?::uuid AND d.status<>'COMPLETED' ORDER BY p.id
                    """).queryArguments(requestId).rowMapper((rs, row) -> new PaymentDocument(
                        rs.getObject(1, java.util.UUID.class), rs.getObject(2, java.util.UUID.class), rs.getLong(3),
                        rs.getLong(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8),
                        rs.getString(9), rs.getBigDecimal(10), rs.getBigDecimal(11), rs.getBigDecimal(12), null)).build();
    }

    @Bean
    Step generateDocumentsStep(JobRepository repository, JdbcCursorItemReader<PaymentDocument> pendingDocumentReader,
                               PdfMotorClient processor, DocumentWriter writer,
                               DocumentFailureListener failureListener) {
        return new StepBuilder("generateDocumentsStep", repository)
                .<PaymentDocument, PaymentDocument>chunk(1)
                .reader(pendingDocumentReader).processor(processor::render).writer(writer)
                .faultTolerant().retry(RuntimeException.class).retryLimit(2)
                .skip(RuntimeException.class).skipLimit(Integer.MAX_VALUE)
                .listener(failureListener).build();
    }

    @Bean
    Step completeRequestStep(JobRepository repository, PlatformTransactionManager transactionManager,
                             CompleteRequestTasklet tasklet) {
        return new StepBuilder("completeRequestStep", repository).tasklet(tasklet, transactionManager).build();
    }
}
