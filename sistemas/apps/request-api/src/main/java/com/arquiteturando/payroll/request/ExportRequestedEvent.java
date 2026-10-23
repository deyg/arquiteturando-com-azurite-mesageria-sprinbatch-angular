package com.arquiteturando.payroll.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExportRequestedEvent(int eventVersion, UUID requestId, Instant createdAt, List<Long> employeeIds,
                                   String startPeriod, String endPeriod, List<String> paymentTypes) {}
