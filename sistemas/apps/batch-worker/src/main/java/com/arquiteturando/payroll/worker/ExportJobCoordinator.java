package com.arquiteturando.payroll.worker;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class ExportJobCoordinator {
    private static final Logger log = LoggerFactory.getLogger(ExportJobCoordinator.class);
    private final JobLauncher launcher;
    private final Job job;
    private final JobExplorer explorer;
    private final JdbcClient jdbc;
    private final io.micrometer.core.instrument.Counter recovered;
    private final io.micrometer.core.instrument.Counter preserved;
    private final Timer recoveryDuration;

    public ExportJobCoordinator(JobLauncher launcher, Job exportPayrollDocumentsJob,
                                JobExplorer explorer, JdbcClient jdbc, MeterRegistry metrics) {
        this.launcher = launcher;
        this.job = exportPayrollDocumentsJob;
        this.explorer = explorer;
        this.jdbc = jdbc;
        this.recovered=metrics.counter("payroll.batch.jobs.recovered");
        this.preserved=metrics.counter("payroll.batch.documents.preserved");
        this.recoveryDuration=metrics.timer("payroll.batch.recovery.duration");
    }

    public synchronized void launch(UUID requestId) {
        JobParameters parameters = parameters(requestId);
        var instance = explorer.getJobInstance(job.getName(), parameters);
        if (instance != null && explorer.getJobExecutions(instance).stream()
                .anyMatch(execution -> execution.isRunning() || execution.getStatus().name().equals("COMPLETED"))) {
            log.info("job_launch_skipped requestId={} reason=existing_execution", requestId);
            return;
        }
        try {
            var execution = launcher.run(job, parameters);
            log.info("job_launched requestId={} jobExecutionId={} status={}",
                    requestId, execution.getId(), execution.getStatus());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not launch export job for " + requestId, exception);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverPendingRequests() {
        recoveryDuration.record(() -> jdbc.sql("SELECT id FROM solicitacao WHERE status IN ('PENDING','PROCESSING') ORDER BY created_at")
                .query(UUID.class).list().forEach(id -> {
                    long completed=jdbc.sql("SELECT count(*) FROM documento_gerado WHERE solicitacao_id=:id AND status='COMPLETED'")
                            .param("id",id).query(Long.class).single();
                    recovered.increment(); preserved.increment(completed); launch(id);
                }));
    }

    private JobParameters parameters(UUID requestId) {
        return new JobParametersBuilder().addString("requestId", requestId.toString(), true).toJobParameters();
    }
}
