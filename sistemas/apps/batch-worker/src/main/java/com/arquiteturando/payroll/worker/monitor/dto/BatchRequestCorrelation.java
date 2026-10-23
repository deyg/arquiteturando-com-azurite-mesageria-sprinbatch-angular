package com.arquiteturando.payroll.worker.monitor.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BatchRequestCorrelation(UUID requestId, String requestStatus, Instant createdAt, Instant startedAt,
        Instant completedAt, long totalDocuments, long completedDocuments, long failedDocuments,
        List<BatchExecutionSummary> executions) {}
