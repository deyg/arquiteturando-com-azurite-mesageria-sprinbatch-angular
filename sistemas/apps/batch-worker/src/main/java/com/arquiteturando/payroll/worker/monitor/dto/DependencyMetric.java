package com.arquiteturando.payroll.worker.monitor.dto;

import java.time.Instant;

public record DependencyMetric(Dependency dependency, Status status, Instant observedAt,
                               Double latencyMs, Instant lastHealthyAt) {
    public enum Dependency { POSTGRESQL, KAFKA, PDF_MOTOR, AZURITE }
    public enum Status { UP, DOWN, DEGRADED, UNKNOWN }
}
