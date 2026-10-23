package com.arquiteturando.payroll.worker.monitor.dto;

import java.util.List;

public record BatchMetricsSummary(MetricWindow window, PerformanceMetrics performance,
        ThroughputMetrics throughput, ResilienceMetrics resilience, List<DependencyMetric> dependencies) {}
