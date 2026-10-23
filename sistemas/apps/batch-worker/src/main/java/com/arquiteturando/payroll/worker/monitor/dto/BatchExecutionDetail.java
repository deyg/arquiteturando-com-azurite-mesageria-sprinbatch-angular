package com.arquiteturando.payroll.worker.monitor.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BatchExecutionDetail(String jobName, long jobInstanceId, long jobExecutionId, UUID requestId,
        BatchStatus status, Instant createTime, Instant startTime, Instant endTime, Long durationMs,
        String exitCode, Instant lastUpdated, String exitDescription, Map<String, String> parameters,
        List<BatchStepExecutionView> steps) {}
