package com.arquiteturando.payroll.worker;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentDocument(UUID documentId, UUID requestId, long paymentId, long employeeId,
                              String employeeName, String registration, String period, String type,
                              String description, BigDecimal grossAmount, BigDecimal discounts,
                              BigDecimal netAmount, byte[] pdf) {
    PaymentDocument withPdf(byte[] content) {
        return new PaymentDocument(documentId, requestId, paymentId, employeeId, employeeName, registration,
                period, type, description, grossAmount, discounts, netAmount, content);
    }
}
