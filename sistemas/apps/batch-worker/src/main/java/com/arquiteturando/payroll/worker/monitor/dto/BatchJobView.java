package com.arquiteturando.payroll.worker.monitor.dto;

import java.time.Instant;

public record BatchJobView(String jobName, long instanceCount, Long latestExecutionId,
                           BatchStatus latestStatus, Instant latestStartTime) {}
