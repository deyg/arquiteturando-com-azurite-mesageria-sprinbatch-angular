package com.arquiteturando.payroll.worker;

import org.springframework.batch.core.configuration.support.JdbcDefaultBatchConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BatchInfrastructureConfiguration extends JdbcDefaultBatchConfiguration {
}
