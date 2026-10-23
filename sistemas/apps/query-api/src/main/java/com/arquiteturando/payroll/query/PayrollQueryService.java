package com.arquiteturando.payroll.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class PayrollQueryService {
    private final JdbcClient jdbc;

    public PayrollQueryService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<EmployeeSummary> employees(String startPeriod, String endPeriod, List<String> paymentTypes) {
        return jdbc.sql("""
                SELECT e.id, e.matricula, e.nome_ficticio, COUNT(p.id) payment_count
                  FROM empregado e
                  JOIN comprovante_pagamento p ON p.empregado_id = e.id
                 WHERE e.ativo
                   AND p.competencia BETWEEN :startPeriod AND :endPeriod
                   AND p.tipo IN (:types)
                 GROUP BY e.id, e.matricula, e.nome_ficticio
                 ORDER BY e.id
                """)
                .param("startPeriod", startPeriod)
                .param("endPeriod", endPeriod)
                .param("types", paymentTypes)
                .query((rs, row) -> new EmployeeSummary(
                        rs.getLong("id"), rs.getString("matricula"),
                        rs.getString("nome_ficticio"), rs.getInt("payment_count")))
                .list();
    }

    public List<PaymentView> payments(long employeeId, String startPeriod, String endPeriod, List<String> paymentTypes) {
        return jdbc.sql("""
                SELECT id, empregado_id, competencia, tipo, descricao,
                       valor_bruto, descontos, valor_liquido, data_pagamento
                  FROM comprovante_pagamento
                 WHERE empregado_id = :employeeId
                   AND competencia BETWEEN :startPeriod AND :endPeriod
                   AND tipo IN (:types)
                 ORDER BY competencia, tipo, id
                """)
                .param("employeeId", employeeId)
                .param("startPeriod", startPeriod)
                .param("endPeriod", endPeriod)
                .param("types", paymentTypes)
                .query((rs, row) -> new PaymentView(
                        rs.getLong("id"), rs.getLong("empregado_id"), rs.getString("competencia"),
                        rs.getString("tipo"), rs.getString("descricao"), rs.getBigDecimal("valor_bruto"),
                        rs.getBigDecimal("descontos"), rs.getBigDecimal("valor_liquido"),
                        rs.getObject("data_pagamento", LocalDate.class)))
                .list();
    }

    public record EmployeeSummary(long employeeId, String registration, String name, int paymentCount) {}
    public record PaymentView(long paymentId, long employeeId, String period, String type, String description,
                              BigDecimal grossAmount, BigDecimal discounts, BigDecimal netAmount, LocalDate paymentDate) {}
}
