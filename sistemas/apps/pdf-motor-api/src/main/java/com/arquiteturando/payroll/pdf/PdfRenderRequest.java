package com.arquiteturando.payroll.pdf;

import java.math.BigDecimal;

public record PdfRenderRequest(long paymentId, long employeeId, String employeeName, String registration,
                               String period, String type, String description, BigDecimal grossAmount,
                               BigDecimal discounts, BigDecimal netAmount) {}
