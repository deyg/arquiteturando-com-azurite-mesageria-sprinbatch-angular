package com.arquiteturando.payroll.worker.monitor.dto;

public record ThroughputMetrics(MetricWindow window, long jobsStarted, long jobsCompleted, long jobsFailed,
        long jobsPartial, long jobsRunning, long documentsRead, long documentsCompleted, long documentsFailed,
        double documentsPerSecond, double successRate, double failureRate, double partialRate, long backlog) {}
