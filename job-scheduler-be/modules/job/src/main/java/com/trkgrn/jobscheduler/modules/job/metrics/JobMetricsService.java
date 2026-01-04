package com.trkgrn.jobscheduler.modules.job.metrics;

import com.trkgrn.jobscheduler.modules.job.model.CronJobModel;
import com.trkgrn.jobscheduler.modules.job.model.CronJobStatus;
import com.trkgrn.jobscheduler.modules.job.model.JobExecutionModel;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class JobMetricsService {

    private static final Logger LOG = LoggerFactory.getLogger(JobMetricsService.class);

    private final MeterRegistry meterRegistry;
    
    // Gauges - Active executions
    private final AtomicInteger activeExecutionsCount = new AtomicInteger(0);

    public JobMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize active executions gauge
        Gauge.builder("job_execution_active", activeExecutionsCount, AtomicInteger::get)
                .description("Number of currently active job executions")
                .tag("application", "job-scheduler")
                .register(meterRegistry);
        
        LOG.info("JobMetricsService initialized");
    }

    public void recordExecutionStart(Long executionId, CronJobModel cronJobModel) {
        activeExecutionsCount.incrementAndGet();
        LOG.debug("Recorded execution start: executionId={}, jobName={}", executionId, cronJobModel.getJobBeanName());
    }

    public void recordExecutionComplete(Long executionId, CronJobModel cronJobModel, 
                                       JobExecutionModel execution) {
        try {
            // Record execution count
            String status = execution.getStatus() != null ? execution.getStatus().name() : "UNKNOWN";
            String jobName = cronJobModel.getJobBeanName() != null ? cronJobModel.getJobBeanName() : "unknown";
            Long cronJobId = cronJobModel.getId();
            
            Counter.builder("job_execution_total")
                    .description("Total number of job executions")
                    .tag("status", status)
                    .tag("job_name", jobName)
                    .tag("cron_job_id", cronJobId != null ? cronJobId.toString() : "unknown")
                    .tag("application", "job-scheduler")
                    .register(meterRegistry)
                    .increment();
            
            // Decrement active executions
            activeExecutionsCount.decrementAndGet();
            
            LOG.debug("Recorded execution complete: executionId={}, status={}, jobName={}", 
                    executionId, status, jobName);
        } catch (Exception e) {
            LOG.error("Error recording execution metrics for executionId={}", executionId, e);
        }
    }

    public void updateJobStatus(CronJobStatus oldStatus, CronJobStatus newStatus) {
        LOG.debug("Job status changed: {} -> {}", oldStatus, newStatus);
    }

    public void cleanupExecution(Long executionId) {
        int current = activeExecutionsCount.get();
        if (current > 0) {
            activeExecutionsCount.decrementAndGet();
        }
        LOG.debug("Cleaned up execution tracking: executionId={}", executionId);
    }
}

