package com.arquiteturando.payroll.worker.monitor.dto;

import java.time.Instant;

public record MetricWindow(Instant from, Instant to) {}
