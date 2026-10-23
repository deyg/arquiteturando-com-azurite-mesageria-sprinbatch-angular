package com.arquiteturando.payroll.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PayrollQueryServiceTest {
    @Autowired PayrollQueryService service;

    @Test
    void returnsNoEmployeesOutsideSeedPeriod() {
        assertThat(service.employees("2025-01", "2025-12", List.of("SALARY"))).isEmpty();
    }

    @Test
    void returnsOneEmployeeForNarrowFilter() {
        assertThat(service.employees("2026-06", "2026-06", List.of("SALARY")))
                .extracting(PayrollQueryService.EmployeeSummary::employeeId)
                .containsExactly(105L);
    }

    @Test
    void returnsAllEmployeesForInclusivePeriod() {
        assertThat(service.employees("2026-01", "2026-06", List.of("SALARY", "BENEFIT"))).hasSize(5);
    }

    @Test
    void returnsTypedPaymentsWithCorrectAmounts() {
        var payments = service.payments(101, "2026-01", "2026-01", List.of("SALARY"));
        assertThat(payments).hasSize(1);
        assertThat(payments.getFirst().netAmount()).isEqualByComparingTo("5250.00");
    }
}
