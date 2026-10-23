package com.arquiteturando.payroll.worker.monitor.dto;

public record ResilienceMetrics(MetricWindow window, long pdfRetries, long pdfFailures, long blobRetries,
        long sqlFailuresAfterUpload, long skippedItems, long rollbacks, long kafkaRedeliveries,
        long recoveredJobs, long preservedDocuments, double averageRecoveryTimeMs) {}
