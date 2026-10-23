package com.arquiteturando.payroll.worker;

import org.springframework.batch.core.listener.SkipListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class DocumentFailureListener implements SkipListener<PaymentDocument, PaymentDocument> {
    private final JdbcClient jdbc;

    public DocumentFailureListener(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void onSkipInProcess(PaymentDocument item, Throwable error) {
        markFailed(item, error);
    }

    @Override
    public void onSkipInWrite(PaymentDocument item, Throwable error) {
        markFailed(item, error);
    }

    private void markFailed(PaymentDocument item, Throwable error) {
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        jdbc.sql("""
                UPDATE documento_gerado SET status='FAILED',error_message=:message,updated_at=CURRENT_TIMESTAMP
                WHERE id=:id AND status<>'COMPLETED'
                """).param("message", message.substring(0, Math.min(message.length(), 1000)))
                .param("id", item.documentId()).update();
    }
}
