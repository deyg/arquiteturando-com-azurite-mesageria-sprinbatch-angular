package com.arquiteturando.payroll.request;

import java.util.List;

public record ExportRequest(List<Long> employeeIds, String startPeriod, String endPeriod, List<String> paymentTypes) {}
