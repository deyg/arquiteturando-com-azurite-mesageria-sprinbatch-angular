package com.arquiteturando.payroll.worker.monitor.dto;

import java.time.Instant;
import java.util.UUID;

public record BatchExecutionSummary(String jobName, long jobInstanceId, long jobExecutionId, UUID requestId,
                                    BatchStatus status, Instant createTime, Instant startTime, Instant endTime,
                                    Long durationMs, String exitCode) {}
