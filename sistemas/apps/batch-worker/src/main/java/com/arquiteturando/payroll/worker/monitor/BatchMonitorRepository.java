package com.arquiteturando.payroll.worker.monitor;

import com.arquiteturando.payroll.worker.monitor.dto.BatchExecutionSummary;
import com.arquiteturando.payroll.worker.monitor.dto.BatchExecutionDetail;
import com.arquiteturando.payroll.worker.monitor.dto.BatchStepExecutionView;
import com.arquiteturando.payroll.worker.monitor.dto.BatchJobView;
import com.arquiteturando.payroll.worker.monitor.dto.BatchStatus;
import com.arquiteturando.payroll.worker.monitor.dto.BatchRequestCorrelation;
import com.arquiteturando.payroll.worker.monitor.dto.PageResponse;
import com.arquiteturando.payroll.worker.monitor.dto.PerformanceMetrics;
import com.arquiteturando.payroll.worker.monitor.dto.MetricWindow;
import com.arquiteturando.payroll.worker.monitor.dto.ThroughputMetrics;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Repository;

@Repository
public class BatchMonitorRepository {
    private final JdbcClient jdbc;
    private final JobRepository jobs;

    public BatchMonitorRepository(JdbcClient jdbc, JobRepository jobs) { this.jdbc = jdbc; this.jobs = jobs; }

    public BatchExecutionDetail execution(long id) {
        JobExecution e = jobs.getJobExecution(id);
        if (e == null) return null;
        var parameters = e.getJobParameters().parameters().stream().collect(java.util.stream.Collectors.toMap(
                p -> p.name(), p -> String.valueOf(p.value())));
        UUID requestId = parameters.containsKey("requestId") ? UUID.fromString(parameters.get("requestId")) : null;
        List<BatchStepExecutionView> steps = e.getStepExecutions().stream().map(this::step).toList();
        Instant start=utc(e.getStartTime()), end=utc(e.getEndTime());
        return new BatchExecutionDetail(e.getJobInstance().getJobName(),e.getJobInstanceId(),e.getId(),requestId,
                status(e.getStatus().name()),utc(e.getCreateTime()),start,end,duration(start,end),
                e.getExitStatus().getExitCode(),utc(e.getLastUpdated()),limit(e.getExitStatus().getExitDescription()),
                parameters,steps);
    }

    public List<BatchStepExecutionView> steps(long id) {
        var detail=execution(id); return detail == null ? null : detail.steps();
    }

    private BatchStepExecutionView step(StepExecution s) {
        Instant start=utc(s.getStartTime()), end=utc(s.getEndTime());
        return new BatchStepExecutionView(s.getId(),s.getStepName(),status(s.getStatus().name()),start,end,
                duration(start,end),s.getReadCount(),s.getWriteCount(),s.getFilterCount(),s.getSkipCount(),
                s.getReadSkipCount(),s.getProcessSkipCount(),s.getWriteSkipCount(),s.getCommitCount(),
                s.getRollbackCount(),s.getExitStatus().getExitCode(),limit(s.getExitStatus().getExitDescription()));
    }

    private static Instant utc(java.time.LocalDateTime value) {
        return value == null ? null : value.toInstant(java.time.ZoneOffset.UTC);
    }
    private static Long duration(Instant start, Instant end) {
        return start == null || end == null ? null : end.toEpochMilli()-start.toEpochMilli();
    }
    private static String limit(String value) { return value == null || value.length()<=4000 ? value : value.substring(0,4000); }

    public List<BatchJobView> jobs() {
        return jdbc.sql("""
                SELECT i.job_name,count(DISTINCT i.job_instance_id),
                  (array_agg(e.job_execution_id ORDER BY e.create_time DESC))[1],
                  (array_agg(e.status ORDER BY e.create_time DESC))[1],
                  (array_agg(e.start_time ORDER BY e.create_time DESC))[1]
                FROM batch_job_instance i LEFT JOIN batch_job_execution e USING(job_instance_id)
                GROUP BY i.job_name ORDER BY i.job_name
                """).query((rs, row) -> new BatchJobView(rs.getString(1), rs.getLong(2), nullableLong(rs, 3),
                        status(rs.getString(4)), instant(rs, 5))).list();
    }

    public BatchRequestCorrelation request(UUID id) {
        var rows=jdbc.sql("""
                SELECT id,status,created_at,started_at,completed_at,total_documents,completed_documents,failed_documents
                FROM solicitacao WHERE id=:id
                """).param("id",id).query((rs,row)->new BatchRequestCorrelation(id,rs.getString(2),instant(rs,3),
                        instant(rs,4),instant(rs,5),rs.getLong(6),rs.getLong(7),rs.getLong(8),List.of())).list();
        if(rows.isEmpty()) return null;
        var base=rows.getFirst();
        var executions=executions(null,null,id,null,null,0,100).items();
        return new BatchRequestCorrelation(id,base.requestStatus(),base.createdAt(),base.startedAt(),base.completedAt(),
                base.totalDocuments(),base.completedDocuments(),base.failedDocuments(),executions);
    }

    public PerformanceMetrics performance(Instant from, Instant to, String jobName, String stepName) {
        validateWindow(from,to);
        Map<String,Object> p=new HashMap<>(Map.of("from",from,"to",to));
        String jobFilter=""; if(jobName!=null){jobFilter=" AND i.job_name=:jobName";p.put("jobName",jobName);}
        String source=" FROM batch_job_execution e JOIN batch_job_instance i USING(job_instance_id)";
        String stepFilter="";
        if(stepName!=null){source=" FROM batch_step_execution e JOIN batch_job_execution je USING(job_execution_id) JOIN batch_job_instance i ON i.job_instance_id=je.job_instance_id";stepFilter=" AND e.step_name=:stepName";p.put("stepName",stepName);}
        String sql="""
                SELECT count(*),coalesce(avg(extract(epoch from (e.end_time-e.start_time))*1000),0),
                 coalesce(min(extract(epoch from (e.end_time-e.start_time))*1000),0),
                 coalesce(max(extract(epoch from (e.end_time-e.start_time))*1000),0),
                 coalesce(percentile_cont(.5) within group(order by extract(epoch from (e.end_time-e.start_time))*1000),0),
                 coalesce(percentile_cont(.95) within group(order by extract(epoch from (e.end_time-e.start_time))*1000),0),
                 coalesce(percentile_cont(.99) within group(order by extract(epoch from (e.end_time-e.start_time))*1000),0)
                """+source+" WHERE e.start_time>=:from AND e.start_time<=:to AND e.end_time IS NOT NULL"+jobFilter+stepFilter;
        return jdbc.sql(sql).params(p).query((rs,row)->new PerformanceMetrics(new MetricWindow(from,to),rs.getLong(1),
                rs.getDouble(2),rs.getLong(3),rs.getLong(4),rs.getDouble(5),rs.getDouble(6),rs.getDouble(7),
                0,0,0,0,0,0)).single();
    }

    public ThroughputMetrics throughput(Instant from, Instant to, String jobName) {
        validateWindow(from,to); Map<String,Object> p=new HashMap<>(Map.of("from",from,"to",to));
        String filter=""; if(jobName!=null){filter=" AND i.job_name=:jobName";p.put("jobName",jobName);}
        long[] jobs=jdbc.sql("""
                SELECT count(*),count(*) filter(where e.status='COMPLETED'),count(*) filter(where e.status='FAILED'),
                 count(*) filter(where e.status in ('STARTING','STARTED','STOPPING'))
                FROM batch_job_execution e JOIN batch_job_instance i USING(job_instance_id)
                WHERE e.create_time>=:from AND e.create_time<=:to
                """+filter).params(p).query((rs,row)->new long[]{rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getLong(4)}).single();
        long[] domain=jdbc.sql("""
                SELECT count(*) filter(where status='PARTIAL'),coalesce(sum(total_documents),0),
                 coalesce(sum(completed_documents),0),coalesce(sum(failed_documents),0),
                 count(*) filter(where status in ('PENDING','PROCESSING'))
                FROM solicitacao WHERE created_at>=:from AND created_at<=:to
                """).params(Map.of("from",from,"to",to)).query((rs,row)->new long[]{rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getLong(4),rs.getLong(5)}).single();
        long finalized=jobs[1]+jobs[2]; double seconds=Math.max(1,java.time.Duration.between(from,to).toMillis()/1000d);
        return new ThroughputMetrics(new MetricWindow(from,to),jobs[0],jobs[1],jobs[2],domain[0],jobs[3],domain[1],
                domain[2],domain[3],domain[2]/seconds,rate(jobs[1],finalized),rate(jobs[2],finalized),
                rate(domain[0],finalized+domain[0]),domain[4]);
    }
    private static double rate(long part,long total){return total==0?0:part/(double)total;}

    static void validateWindow(Instant from, Instant to) {
        if(from==null || to==null || !from.isBefore(to) || java.time.Duration.between(from,to).toDays()>31)
            throw new IllegalArgumentException("Invalid metrics window; maximum is 31 days");
    }

    public PageResponse<BatchExecutionSummary> executions(String jobName, BatchStatus status, UUID requestId,
            Instant startedFrom, Instant startedTo, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid pagination");
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        condition(where, params, jobName != null, " AND i.job_name=:jobName", "jobName", jobName);
        condition(where, params, status != null, " AND e.status=:status", "status", status == null ? null : status.name());
        condition(where, params, requestId != null, """
                 AND EXISTS (SELECT 1 FROM batch_job_execution_params p WHERE p.job_execution_id=e.job_execution_id
                  AND p.parameter_name='requestId' AND p.parameter_value=:requestId)
                """, "requestId", requestId == null ? null : requestId.toString());
        condition(where, params, startedFrom != null, " AND e.start_time>=:startedFrom", "startedFrom", startedFrom);
        condition(where, params, startedTo != null, " AND e.start_time<=:startedTo", "startedTo", startedTo);
        String from = " FROM batch_job_execution e JOIN batch_job_instance i USING(job_instance_id)" + where;
        long total = jdbc.sql("SELECT count(*)" + from).params(params).query(Long.class).single();
        params.put("limit", size); params.put("offset", page * size);
        List<BatchExecutionSummary> items = jdbc.sql("""
                SELECT i.job_name,e.job_instance_id,e.job_execution_id,
                 (SELECT p.parameter_value FROM batch_job_execution_params p WHERE p.job_execution_id=e.job_execution_id
                   AND p.parameter_name='requestId' LIMIT 1),
                 e.status,e.create_time,e.start_time,e.end_time,e.exit_code
                """ + from + " ORDER BY e.create_time DESC,e.job_execution_id DESC LIMIT :limit OFFSET :offset")
                .params(params).query(this::summary).list();
        return new PageResponse<>(items, page, size, total, (int) Math.ceil(total / (double) size));
    }

    private BatchExecutionSummary summary(ResultSet rs, int row) throws SQLException {
        Instant start = instant(rs, 7), end = instant(rs, 8);
        return new BatchExecutionSummary(rs.getString(1), rs.getLong(2), rs.getLong(3),
                rs.getString(4) == null ? null : UUID.fromString(rs.getString(4)), status(rs.getString(5)),
                instant(rs, 6), start, end, start == null || end == null ? null : end.toEpochMilli() - start.toEpochMilli(),
                rs.getString(9));
    }

    static BatchStatus status(String value) {
        if (value == null) return BatchStatus.UNKNOWN;
        try { return BatchStatus.valueOf(value); } catch (IllegalArgumentException ignored) { return BatchStatus.UNKNOWN; }
    }

    static Instant instant(ResultSet rs, int column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Long nullableLong(ResultSet rs, int column) throws SQLException {
        long value = rs.getLong(column); return rs.wasNull() ? null : value;
    }

    private static void condition(StringBuilder sql, Map<String, Object> params, boolean include,
            String fragment, String name, Object value) {
        if (include) { sql.append(fragment); params.put(name, value); }
    }
}
