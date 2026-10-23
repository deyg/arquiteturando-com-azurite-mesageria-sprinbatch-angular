package com.arquiteturando.payroll.worker.monitor;

import com.arquiteturando.payroll.worker.monitor.dto.BatchExecutionSummary;
import com.arquiteturando.payroll.worker.monitor.dto.BatchJobView;
import com.arquiteturando.payroll.worker.monitor.dto.BatchStatus;
import com.arquiteturando.payroll.worker.monitor.dto.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/batch")
public class BatchMonitorController {
    private final BatchMonitorRepository repository;
    public BatchMonitorController(BatchMonitorRepository repository) { this.repository = repository; }

    @GetMapping("/jobs")
    List<BatchJobView> jobs() { return repository.jobs(); }

    @GetMapping("/executions")
    PageResponse<BatchExecutionSummary> executions(@RequestParam(required=false) String jobName,
            @RequestParam(required=false) BatchStatus status, @RequestParam(required=false) UUID requestId,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant startedFrom,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant startedTo,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return repository.executions(jobName, status, requestId, startedFrom, startedTo, page, size);
    }

    @GetMapping("/executions/{executionId}")
    Object execution(@PathVariable long executionId) {
        var result=repository.execution(executionId);
        if(result==null) throw new ResponseStatusException(NOT_FOUND,"Batch execution not found");
        return result;
    }

    @GetMapping("/executions/{executionId}/steps")
    Object steps(@PathVariable long executionId) {
        var result=repository.steps(executionId);
        if(result==null) throw new ResponseStatusException(NOT_FOUND,"Batch execution not found");
        return result;
    }

    @GetMapping("/requests/{requestId}")
    Object request(@PathVariable UUID requestId) {
        var result=repository.request(requestId);
        if(result==null) throw new ResponseStatusException(NOT_FOUND,"Request not found");
        return result;
    }
}
