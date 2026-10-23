package com.arquiteturando.payroll.worker.monitor;

import com.arquiteturando.payroll.worker.monitor.dto.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BatchMetricsService {
    private final BatchMonitorRepository repository;
    private final MeterRegistry metrics;
    public BatchMetricsService(BatchMonitorRepository repository,MeterRegistry metrics){this.repository=repository;this.metrics=metrics;}

    public PerformanceMetrics performance(Instant from,Instant to,String job,String step){
        var historical=repository.performance(from,to,job,step);
        Timer pdf=metrics.find("payroll.batch.pdf.duration").timer();
        Timer blob=metrics.find("payroll.batch.blob.duration").timer();
        Timer sql=metrics.find("payroll.batch.sql.duration").timer();
        return new PerformanceMetrics(historical.window(),historical.sampleCount(),historical.averageJobDurationMs(),
                historical.minJobDurationMs(),historical.maxJobDurationMs(),historical.p50JobDurationMs(),
                historical.p95JobDurationMs(),historical.p99JobDurationMs(),meanMs(pdf),percentileMs(pdf,.95),
                meanMs(blob),meanMs(sql),historical.averageQueueTimeMs(),historical.averageEndToEndTimeMs());
    }

    public ResilienceMetrics resilience(Instant from,Instant to,String job){
        BatchMonitorRepository.ResilienceCounts c=repository.resilienceCounts(from,to,job);
        return new ResilienceMetrics(new MetricWindow(from,to),count("payroll.batch.pdf.retries"),
                count("payroll.batch.pdf.failures"),count("payroll.batch.blob.retries"),
                count("payroll.batch.sql.failures.after.upload"),c.skips(),c.rollbacks(),
                count("payroll.batch.kafka.redeliveries"),count("payroll.batch.jobs.recovered"),
                count("payroll.batch.documents.preserved"),meanMs(metrics.find("payroll.batch.recovery.duration").timer()));
    }

    public List<DependencyMetric> dependencies(){
        Instant now=Instant.now();
        return List.of(new DependencyMetric(DependencyMetric.Dependency.POSTGRESQL,DependencyMetric.Status.UP,now,null,now),
                new DependencyMetric(DependencyMetric.Dependency.KAFKA,DependencyMetric.Status.UNKNOWN,now,null,null),
                new DependencyMetric(DependencyMetric.Dependency.PDF_MOTOR,DependencyMetric.Status.UNKNOWN,now,null,null),
                new DependencyMetric(DependencyMetric.Dependency.AZURITE,DependencyMetric.Status.UNKNOWN,now,null,null));
    }

    public BatchMetricsSummary summary(Instant from,Instant to,String job,BatchStatus status){
        return new BatchMetricsSummary(new MetricWindow(from,to),performance(from,to,job,null),
                repository.throughput(from,to,job),resilience(from,to,job),dependencies());
    }
    private long count(String name){var c=metrics.find(name).counter();return c==null?0:Math.round(c.count());}
    private static double meanMs(Timer t){return t==null?0:t.mean(java.util.concurrent.TimeUnit.MILLISECONDS);}
    private static double percentileMs(Timer t,double percentile){
        if(t==null)return 0; return java.util.Arrays.stream(t.takeSnapshot().percentileValues())
                .filter(v->Math.abs(v.percentile()-percentile)<.001).mapToDouble(v->v.value(java.util.concurrent.TimeUnit.MILLISECONDS))
                .findFirst().orElse(0);
    }
}
