package com.arquiteturando.payroll.worker;

import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchJobConfiguration {
    @Bean
    Step prepareRequestStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("prepareRequestStep", jobRepository)
                .tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
                .build();
    }

    @Bean
    Job exportPayrollDocumentsJob(JobRepository jobRepository, Step prepareRequestStep) {
        return new JobBuilder("exportPayrollDocumentsJob", jobRepository)
                .start(prepareRequestStep)
                .build();
    }
}
