package com.trkgrn.jobscheduler.modules.job.scheduler;

import ch.qos.logback.classic.Level;
import com.trkgrn.jobscheduler.modules.job.api.JobResult;
import com.trkgrn.jobscheduler.modules.job.logging.JobLogCollector;
import com.trkgrn.jobscheduler.modules.job.metrics.JobMetricsService;
import com.trkgrn.jobscheduler.modules.job.model.CronJobModel;
import com.trkgrn.jobscheduler.modules.job.model.CronJobStatus;
import com.trkgrn.jobscheduler.modules.job.model.JobExecutionModel;
import com.trkgrn.jobscheduler.modules.job.registry.JobRegistry;
import com.trkgrn.jobscheduler.modules.job.repository.CronJobRepository;
import com.trkgrn.jobscheduler.modules.job.service.JobExecutionService;
import com.trkgrn.jobscheduler.modules.job.util.NodeIdentifier;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CronJobQuartzJob implements Job {

    private static final Logger LOG = LoggerFactory.getLogger(CronJobQuartzJob.class);

    private final CronJobRepository cronJobRepository;
    private final JobRegistry jobRegistry;
    private final JobExecutionService jobExecutionService;
    private final JobLogCollector jobLogCollector;
    private final NodeIdentifier nodeIdentifier;
    private final JobMetricsService jobMetricsService;
    private final TransactionTemplate transactionTemplate;

    public CronJobQuartzJob(CronJobRepository cronJobRepository, JobRegistry jobRegistry,
                            JobExecutionService jobExecutionService, JobLogCollector jobLogCollector,
                            NodeIdentifier nodeIdentifier, JobMetricsService jobMetricsService,
                            PlatformTransactionManager transactionManager) {
        this.cronJobRepository = cronJobRepository;
        this.jobRegistry = jobRegistry;
        this.jobExecutionService = jobExecutionService;
        this.jobLogCollector = jobLogCollector;
        this.nodeIdentifier = nodeIdentifier;
        this.jobMetricsService = jobMetricsService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long cronJobId = context.getJobDetail().getJobDataMap().getLong("cronJobId");
        
        LOG.info("Quartz executing CronJob with ID: {}", cronJobId);

        try {
            // executeCronJobWithRetry() already handles transactions internally
            // No need for outer transaction wrapper
            executeCronJobWithRetry(cronJobId);
        } catch (Exception e) {
            LOG.error("Error executing CronJob with ID: {}", cronJobId, e);
            
            // Try to update status even if there was an error (in a separate transaction)
            try {
                transactionTemplate.execute(new TransactionCallback<Void>() {
                    @Override
                    public Void doInTransaction(TransactionStatus status) {
                        try {
                            updateCronJobStatus(cronJobId, CronJobStatus.FAILED, "EXCEPTION: " + e.getMessage());
                        } catch (Exception updateException) {
                            LOG.error("Failed to update CronJob status after error", updateException);
                            status.setRollbackOnly();
                        }
                        return null;
                    }
                });
            } catch (Exception updateException) {
                LOG.error("Failed to update CronJob status after error", updateException);
            }
        }
    }

    @Retryable(
        value = {OptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public void executeCronJobWithRetry(Long cronJobId) {
        // This transaction should be short to avoid blocking other operations
        ExecutionContext executionContext = transactionTemplate.execute(new TransactionCallback<ExecutionContext>() {
            @Override
            public ExecutionContext doInTransaction(TransactionStatus status) {
                CronJobModel cronJobModel = validateAndLoadCronJob(cronJobId);
                if (cronJobModel == null) {
                    return null;
                }

                JobExecutionModel execution = createExecutionRecord(cronJobModel);
                String correlationId = execution.getCorrelationId();
                
                startLogCollection(execution, correlationId, cronJobModel);
                
                // Record metrics: execution start
                jobMetricsService.recordExecutionStart(execution.getId(), cronJobModel);
                jobMetricsService.updateJobStatus(null, CronJobStatus.RUNNING);
                
                return new ExecutionContext(cronJobModel, execution, correlationId);
            }
        });

        if (executionContext == null) {
            return;
        }

        try {
            JobResult result = executeJob(executionContext.cronJobModel, executionContext.execution);
            
            transactionTemplate.execute(new TransactionCallback<Void>() {
                @Override
                public Void doInTransaction(TransactionStatus status) {
                    // Reload entities to ensure they are managed
                    CronJobModel cronJobModel = cronJobRepository.findById(executionContext.cronJobModel.getId())
                            .orElse(executionContext.cronJobModel);
                    JobExecutionModel execution = jobExecutionService.findById(executionContext.execution.getId())
                            .orElse(executionContext.execution);
                    
                    boolean isCancelled = execution.getStatus() == JobExecutionModel.Status.CANCELLED;
                    boolean isAbortedFromResult = result != null && !result.getSuccess() && 
                            result.getMessage() != null && 
                            (result.getMessage().toLowerCase().contains("aborted") || 
                             result.getMessage().toLowerCase().contains("cancelled"));
                    
                    CronJobStatus oldStatus = cronJobModel.getStatus();
                    CronJobStatus newStatus = null;
                    
                    if (isCancelled || isAbortedFromResult) {
                        handleCancelledExecution(cronJobModel, execution, result);
                        newStatus = CronJobStatus.CANCELLED;
                    } else if (result.getSuccess()) {
                        handleSuccessfulExecution(cronJobModel, execution, result);
                        newStatus = CronJobStatus.FINISHED;
                    } else {
                        handleFailedExecution(cronJobModel, execution, result);
                        newStatus = CronJobStatus.FAILED;
                    }
                    
                    // Record metrics: execution complete and status change
                    jobMetricsService.recordExecutionComplete(execution.getId(), cronJobModel, execution);
                    jobMetricsService.updateJobStatus(oldStatus, newStatus);
                    
                    finalizeCronJobExecution(cronJobModel);
                    return null;
                }
            });
        } catch (Exception e) {
            transactionTemplate.execute(new TransactionCallback<Void>() {
                @Override
                public Void doInTransaction(TransactionStatus status) {
                    // Reload entities to ensure they are managed
                    CronJobModel cronJobModel = cronJobRepository.findById(executionContext.cronJobModel.getId())
                            .orElse(executionContext.cronJobModel);
                    JobExecutionModel execution = jobExecutionService.findById(executionContext.execution.getId())
                            .orElse(executionContext.execution);
                    
                    CronJobStatus oldStatus = cronJobModel.getStatus();
                    handleExceptionExecution(cronJobModel, execution, e);
                    
                    // Record metrics: execution complete and status change
                    jobMetricsService.recordExecutionComplete(execution.getId(), cronJobModel, execution);
                    jobMetricsService.updateJobStatus(oldStatus, CronJobStatus.FAILED);
                    
                    finalizeCronJobExecution(cronJobModel);
                    return null;
                }
            });
        } finally {
            clearMDC();
        }
    }

    private static class ExecutionContext {
        final CronJobModel cronJobModel;
        final JobExecutionModel execution;
        final String correlationId;

        ExecutionContext(CronJobModel cronJobModel, JobExecutionModel execution, String correlationId) {
            this.cronJobModel = cronJobModel;
            this.execution = execution;
            this.correlationId = correlationId;
        }
    }

    private CronJobModel validateAndLoadCronJob(Long cronJobId) {
        // This ensures only one execution (manual or scheduled) can run at a time
        CronJobModel cronJobModel = cronJobRepository.findByIdWithLock(cronJobId).orElse(null);
        if (cronJobModel == null) {
            LOG.error("CronJob not found with ID: {}", cronJobId);
            return null;
        }

        if (!cronJobModel.getEnabled()) {
            LOG.warn("CronJob {} is disabled, skipping execution", cronJobModel.getCode());
            return null;
        }

        if (cronJobModel.getStatus() == CronJobStatus.RUNNING) {
            List<JobExecutionModel> runningExecutions = jobExecutionService.findRunningByJobDefinitionId(cronJobId);
            if (runningExecutions.isEmpty()) {
                // Stuck state: Status is RUNNING but no active executions found
                // This can happen if job was interrupted or cleanup didn't run
                LOG.warn("CronJob {} status is RUNNING but no active executions found. This indicates a stuck state. " +
                        "Status: {}, LastStartTime: {}, LastEndTime: {}. " +
                        "Resetting status to FAILED to allow future executions.",
                        cronJobModel.getCode(),
                        cronJobModel.getStatus(),
                        cronJobModel.getLastStartTime(),
                        cronJobModel.getLastEndTime());
                
                // Fix stuck state: Reset status to FAILED
                cronJobModel.setStatus(CronJobStatus.FAILED);
                cronJobModel.setLastResult("Stuck state detected: Status was RUNNING but no active executions found");
                cronJobRepository.saveAndFlush(cronJobModel);
                
                // Continue with execution instead of returning null
                LOG.info("Stuck state fixed for CronJob {}. Proceeding with scheduled execution.", cronJobModel.getCode());
            } else {
                LOG.warn("CronJob {} is already running (possibly manual execution), skipping scheduled execution. " +
                        "Active executions: {}",
                        cronJobModel.getCode(),
                        runningExecutions.stream()
                                .map(e -> String.format("ID=%d, Status=%s, StartedAt=%s, NodeId=%s",
                                        e.getId(), e.getStatus(), e.getStartedAt(), e.getNodeId()))
                                .collect(Collectors.joining(", ")));
                return null;
            }
        }

        List<JobExecutionModel> runningExecutions = jobExecutionService.findRunningByJobDefinitionId(cronJobId);
        if (!runningExecutions.isEmpty()) {
            LOG.warn("CronJob {} has running executions (possibly manual execution), skipping scheduled execution. " +
                    "Active executions: {}",
                    cronJobModel.getCode(),
                    runningExecutions.stream()
                            .map(e -> String.format("ID=%d, Status=%s, StartedAt=%s, NodeId=%s",
                                    e.getId(), e.getStatus(), e.getStartedAt(), e.getNodeId()))
                            .collect(Collectors.joining(", ")));
            return null;
        }

        // Immediately set status to RUNNING to prevent other executions
        // This is done while holding the pessimistic lock
        cronJobModel.setStatus(CronJobStatus.RUNNING);
        cronJobModel.setLastStartTime(OffsetDateTime.now());
        cronJobModel = cronJobRepository.saveAndFlush(cronJobModel);

        return cronJobModel;
    }

    private JobExecutionModel createExecutionRecord(CronJobModel cronJobModel) {
        JobExecutionModel execution = new JobExecutionModel();
        execution.setJobDefinition(cronJobModel);
        execution.setStatus(JobExecutionModel.Status.RUNNING);
        execution.setStartedAt(OffsetDateTime.now());
        execution.setLogLevel(cronJobModel.getLogLevel().name());
        execution.setNodeId(nodeIdentifier.getNodeId());
        
        String correlationId = "QUARTZ-" + System.currentTimeMillis();
        execution.setCorrelationId(correlationId);
        
        return jobExecutionService.save(execution);
    }

    private void startLogCollection(JobExecutionModel execution, String correlationId, CronJobModel cronJobModel) {
        Level jobLogLevel = Level.toLevel(cronJobModel.getLogLevel().name(), Level.INFO);
        jobLogCollector.startLogCollection(execution.getId(), correlationId, jobLogLevel);
        
        MDC.put("correlationId", correlationId);
        MDC.put("executionId", execution.getId().toString());

        LOG.info("Starting scheduled execution of CronJob: {} (Execution ID: {})", 
                cronJobModel.getCode(), execution.getId());
    }

    private JobResult executeJob(CronJobModel cronJobModel, JobExecutionModel execution) {
        LOG.info("Executing job: {} with bean name: {}", cronJobModel.getCode(), cronJobModel.getJobBeanName());
        // Pass execution ID for abortable job support
        JobResult result = jobRegistry.executeJob(cronJobModel, execution.getId());
        LOG.info("Job execution completed. Success: {}, Message: {}", result.getSuccess(), result.getMessage());
        return result;
    }

    private void handleSuccessfulExecution(CronJobModel cronJobModel, JobExecutionModel execution, JobResult result) {
        execution.setStatus(JobExecutionModel.Status.SUCCESS);
        execution.setEndedAt(OffsetDateTime.now());

        cronJobModel.setStatus(CronJobStatus.FINISHED);
        cronJobModel.setLastResult("SUCCESS: " + result.getMessage());
        
        LOG.info("CronJob {} completed successfully: {} (Execution ID: {})", 
                cronJobModel.getCode(), result.getMessage(), execution.getId());

        addSuccessLogs(execution, result);
        persistExecutionWithLogs(execution);
    }

    private void handleFailedExecution(CronJobModel cronJobModel, JobExecutionModel execution, JobResult result) {
        execution.setStatus(JobExecutionModel.Status.FAILED);
        execution.setEndedAt(OffsetDateTime.now());

        cronJobModel.setStatus(CronJobStatus.FAILED);
        cronJobModel.setLastResult("FAILED: " + result.getMessage());
        cronJobModel.setRetryCount(cronJobModel.getRetryCount() + 1);
        
        LOG.error("CronJob {} failed: {} (Execution ID: {})", 
                cronJobModel.getCode(), result.getMessage(), execution.getId());

        addFailureLogs(execution, result);
        persistExecutionWithLogs(execution);
    }

    private void handleCancelledExecution(CronJobModel cronJobModel, JobExecutionModel execution, JobResult result) {
        execution.setStatus(JobExecutionModel.Status.CANCELLED);
        execution.setEndedAt(OffsetDateTime.now());

        cronJobModel.setStatus(CronJobStatus.CANCELLED);
        cronJobModel.setLastResult("CANCELLED: " + (result != null ? result.getMessage() : "Job aborted by user"));
        
        LOG.warn("CronJob {} was cancelled: {} (Execution ID: {})", 
                cronJobModel.getCode(), result != null ? result.getMessage() : "Job aborted", execution.getId());

        addCancelledLogs(execution, result);
        persistExecutionWithLogs(execution);
    }

    private void handleExceptionExecution(CronJobModel cronJobModel, JobExecutionModel execution, Exception e) {
        execution.setStatus(JobExecutionModel.Status.FAILED);
        execution.setEndedAt(OffsetDateTime.now());

        cronJobModel.setStatus(CronJobStatus.FAILED);
        cronJobModel.setLastResult("EXCEPTION: " + e.getMessage());
        cronJobModel.setRetryCount(cronJobModel.getRetryCount() + 1);
        
        LOG.error("CronJob {} failed with exception: {} (Execution ID: {})", 
                cronJobModel.getCode(), e.getMessage(), execution.getId(), e);

        addExceptionLogs(execution, e);
        persistExecutionWithLogs(execution);
    }

    private void addSuccessLogs(JobExecutionModel execution, JobResult result) {
        jobLogCollector.addLog(execution.getId(), "INFO", "Job completed successfully: " + result.getMessage());
        jobLogCollector.addLog(execution.getId(), "INFO", "Execution statistics: " + jobLogCollector.getStatistics());
        jobLogCollector.stopLogCollectionAndPersist(execution.getId(), execution);
    }

    private void addFailureLogs(JobExecutionModel execution, JobResult result) {
        jobLogCollector.addLog(execution.getId(), "ERROR", "Job failed: " + result.getMessage());
        jobLogCollector.addLog(execution.getId(), "ERROR", "Execution statistics: " + jobLogCollector.getStatistics());
        jobLogCollector.stopLogCollectionAndPersist(execution.getId(), execution);
    }

    private void addCancelledLogs(JobExecutionModel execution, JobResult result) {
        jobLogCollector.addLog(execution.getId(), "WARN", "Job was cancelled: " + (result != null ? result.getMessage() : "Job aborted by user"));
        jobLogCollector.addLog(execution.getId(), "INFO", "Execution statistics: " + jobLogCollector.getStatistics());
        jobLogCollector.stopLogCollectionAndPersist(execution.getId(), execution);
    }

    private void addExceptionLogs(JobExecutionModel execution, Exception e) {
        jobLogCollector.addLog(execution.getId(), "ERROR", "Job failed with exception: " + e.getMessage());
        jobLogCollector.stopLogCollectionAndPersist(execution.getId(), execution);
    }

    private void persistExecutionWithLogs(JobExecutionModel execution) {
        jobExecutionService.save(execution);
    }

    private void finalizeCronJobExecution(CronJobModel cronJobModel) {
        cronJobModel.setLastEndTime(OffsetDateTime.now());
        saveCronJobWithOptimisticLocking(cronJobModel);
    }

    private void saveCronJobWithOptimisticLocking(CronJobModel cronJobModel) {
        try {
            cronJobRepository.saveAndFlush(cronJobModel);
        } catch (OptimisticLockingFailureException e) {
            CronJobModel freshCronJob = cronJobRepository.findById(cronJobModel.getId()).orElse(null);
            if (freshCronJob != null) {
                updateCronJobFromModel(freshCronJob, cronJobModel);
                cronJobRepository.saveAndFlush(freshCronJob);
            }
        }
    }

    private void updateCronJobFromModel(CronJobModel target, CronJobModel source) {
        target.setStatus(source.getStatus());
        target.setLastResult(source.getLastResult());
        target.setRetryCount(source.getRetryCount());
        target.setLastStartTime(source.getLastStartTime());
        target.setLastEndTime(source.getLastEndTime());
    }

    private void clearMDC() {
        MDC.remove("correlationId");
        MDC.remove("executionId");
    }

    @Retryable(
        value = {OptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public void updateCronJobStatus(Long cronJobId, CronJobStatus status, String result) {
        // This method is called from within a transactionTemplate.execute() block
        // So we don't need @Transactional annotation here
        CronJobModel cronJobModel = cronJobRepository.findById(cronJobId).orElse(null);
        if (cronJobModel == null) {
            LOG.warn("CronJob not found with ID: {} for status update", cronJobId);
            return;
        }

        updateCronJobStatusFields(cronJobModel, status, result);
        saveCronJobStatusWithRetry(cronJobId, cronJobModel, status, result);
    }

    private void updateCronJobStatusFields(CronJobModel cronJobModel, CronJobStatus status, String result) {
        cronJobModel.setStatus(status);
        cronJobModel.setLastEndTime(OffsetDateTime.now());
        cronJobModel.setLastResult(result);
        cronJobModel.setRetryCount(cronJobModel.getRetryCount() + 1);
    }

    private void saveCronJobStatusWithRetry(Long cronJobId, CronJobModel cronJobModel, 
                                           CronJobStatus status, String result) {
        try {
            cronJobRepository.saveAndFlush(cronJobModel);
        } catch (OptimisticLockingFailureException e) {
            CronJobModel freshCronJob = cronJobRepository.findById(cronJobId).orElse(null);
            if (freshCronJob != null) {
                updateCronJobStatusFields(freshCronJob, status, result);
                cronJobRepository.saveAndFlush(freshCronJob);
            }
        }
    }
}

