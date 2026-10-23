package com.arquiteturando.payroll.worker.monitor.dto;

public record PerformanceMetrics(MetricWindow window, long sampleCount, double averageJobDurationMs,
        long minJobDurationMs, long maxJobDurationMs, double p50JobDurationMs, double p95JobDurationMs,
        double p99JobDurationMs, double averagePdfLatencyMs, double p95PdfLatencyMs,
        double averageBlobLatencyMs, double averageSqlLatencyMs, double averageQueueTimeMs,
        double averageEndToEndTimeMs) {}
