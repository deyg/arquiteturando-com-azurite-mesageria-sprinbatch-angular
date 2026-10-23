package com.arquiteturando.payroll.worker.monitor.dto;

import java.time.Instant;

public record BatchStepExecutionView(long stepExecutionId, String stepName, BatchStatus status,
        Instant startTime, Instant endTime, Long durationMs, long readCount, long writeCount,
        long filterCount, long skipCount, long readSkipCount, long processSkipCount, long writeSkipCount,
        long commitCount, long rollbackCount, String exitCode, String exitDescription) {}
