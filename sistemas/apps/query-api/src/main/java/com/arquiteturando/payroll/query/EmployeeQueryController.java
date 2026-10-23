package com.arquiteturando.payroll.query;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeQueryController {
    private final PayrollQueryService service;

    public EmployeeQueryController(PayrollQueryService service) {
        this.service = service;
    }

    @GetMapping
    public List<PayrollQueryService.EmployeeSummary> employees(
            @RequestParam String startPeriod,
            @RequestParam String endPeriod,
            @RequestParam List<String> paymentTypes) {
        validate(startPeriod, endPeriod, paymentTypes);
        return service.employees(startPeriod, endPeriod, paymentTypes);
    }

    @GetMapping("/{employeeId}/payments")
    public List<PayrollQueryService.PaymentView> payments(
            @PathVariable long employeeId,
            @RequestParam String startPeriod,
            @RequestParam String endPeriod,
            @RequestParam(defaultValue = "SALARY,BENEFIT") List<String> paymentTypes) {
        validate(startPeriod, endPeriod, paymentTypes);
        return service.payments(employeeId, startPeriod, endPeriod, paymentTypes);
    }

    private static void validate(String start, String end, List<String> types) {
        try {
            if (YearMonth.parse(start).isAfter(YearMonth.parse(end))) {
                throw new ResponseStatusException(BAD_REQUEST, "startPeriod must not be after endPeriod");
            }
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(BAD_REQUEST, "periods must use YYYY-MM", exception);
        }
        if (types.isEmpty() || types.stream().anyMatch(type -> !type.equals("SALARY") && !type.equals("BENEFIT"))) {
            throw new ResponseStatusException(BAD_REQUEST, "paymentTypes must contain SALARY and/or BENEFIT");
        }
    }
}
