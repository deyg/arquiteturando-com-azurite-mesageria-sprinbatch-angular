package com.arquiteturando.payroll.worker;

import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    Job exportPayrollDocumentsJob(JobRepository jobRepository, Step prepareRequestStep) {
        return new JobBuilder("exportPayrollDocumentsJob", jobRepository)
                .start(prepareRequestStep)
                .build();
    }
}
