package com.trkgrn.jobscheduler.modules.job.service.impl;

import ch.qos.logback.classic.Level;
import com.trkgrn.jobscheduler.modules.job.logging.JobLogCollector;
import com.trkgrn.jobscheduler.modules.job.metrics.JobMetricsService;
import com.trkgrn.jobscheduler.modules.job.model.CronJobModel;
import com.trkgrn.jobscheduler.modules.job.model.CronJobStatus;
import com.trkgrn.jobscheduler.modules.job.model.JobExecutionModel;
import com.trkgrn.jobscheduler.modules.job.model.TriggerModel;
import com.trkgrn.jobscheduler.modules.job.registry.JobRegistry;
import com.trkgrn.jobscheduler.modules.job.repository.CronJobRepository;
import com.trkgrn.jobscheduler.modules.job.repository.JobExecutionRepository;
import com.trkgrn.jobscheduler.modules.job.repository.TriggerRepository;
import com.trkgrn.jobscheduler.modules.job.scheduler.QuartzJobScheduler;
import com.trkgrn.jobscheduler.modules.job.service.CronJobService;
import com.trkgrn.jobscheduler.modules.job.service.JobExecutionService;
import com.trkgrn.jobscheduler.modules.job.util.NodeIdentifier;
import com.trkgrn.jobscheduler.platform.common.model.exception.JobAlreadyRunningException;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DefaultCronJobService implements CronJobService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultCronJobService.class);

    private final CronJobRepository cronJobRepository;
    private final TriggerRepository triggerRepository;
    private final JobExecutionService jobExecutionService;
    private final JobExecutionRepository jobExecutionRepository;
    private final JobRegistry jobRegistry;
    private final JobLogCollector jobLogCollector;
    private final QuartzJobScheduler quartzJobScheduler;
    private final EntityManager entityManager;
    private final NodeIdentifier nodeIdentifier;
    private final JobMetricsService jobMetricsService;
    private final TransactionTemplate transactionTemplate;

    public DefaultCronJobService(CronJobRepository cronJobRepository, TriggerRepository triggerRepository,
                                 JobExecutionService jobExecutionService, JobExecutionRepository jobExecutionRepository,
                                 JobRegistry jobRegistry, JobLogCollector jobLogCollector,
                                 QuartzJobScheduler quartzJobScheduler, EntityManager entityManager,
                                 NodeIdentifier nodeIdentifier, JobMetricsService jobMetricsService,
                                 PlatformTransactionManager transactionManager) {
        this.cronJobRepository = cronJobRepository;
        this.triggerRepository = triggerRepository;
        this.jobExecutionService = jobExecutionService;
        this.jobExecutionRepository = jobExecutionRepository;
        this.jobRegistry = jobRegistry;
        this.jobLogCollector = jobLogCollector;
        this.quartzJobScheduler = quartzJobScheduler;
        this.entityManager = entityManager;
        this.nodeIdentifier = nodeIdentifier;
        this.jobMetricsService = jobMetricsService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    @Override
    public List<CronJobModel> findAll() {
        return cronJobRepository.findAll();
    }

    @Override
    public Optional<CronJobModel> findById(Long id) {
        return cronJobRepository.findById(id);
    }

    @Override
    public Optional<CronJobModel> findByCode(String code) {
        return cronJobRepository.findByCode(code);
    }

    @Override
    @Transactional
    public CronJobModel save(CronJobModel cronJobModel) {
        return cronJobRepository.save(cronJobModel);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        LOG.info("Deleting CronJob with ID: {}", id);

        // Find the CronJob first
        CronJobModel cronJob = cronJobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CronJob not found with id: " + id));

        try {
            // 1. First, unschedule and delete all triggers from Quartz scheduler
            List<TriggerModel> triggers = triggerRepository.findByCronJobId(id);
            LOG.info("Found {} triggers for CronJob ID: {}", triggers.size(), id);

            for (TriggerModel trigger : triggers) {
                try {
                    // Unschedule from Quartz
                    quartzJobScheduler.unscheduleTrigger(trigger);
                    LOG.info("Unscheduled trigger: {} from Quartz", trigger.getName());
                } catch (Exception e) {
                    LOG.warn("Failed to unschedule trigger {} from Quartz: {}", trigger.getName(), e.getMessage());
                }
            }

            // 2. Delete all triggers from database
            for (TriggerModel trigger : triggers) {
                triggerRepository.deleteById(trigger.getId());
            }
            LOG.info("Deleted {} triggers from database for CronJob ID: {}", triggers.size(), id);

            // 3. Delete all executions (this should be handled by cascade, but let's be explicit)
            List<JobExecutionModel> executions = jobExecutionService.findByJobDefinitionId(id);
            LOG.info("Found {} executions for CronJob ID: {}", executions.size(), id);

            for (JobExecutionModel execution : executions) {
                jobExecutionService.deleteById(execution.getId());
            }
            LOG.info("Deleted {} executions for CronJob ID: {}", executions.size(), id);

            // 4. Finally, delete the CronJob itself
            cronJobRepository.deleteById(id);
            LOG.info("Successfully deleted CronJob with ID: {}", id);

        } catch (Exception e) {
            LOG.error("Failed to delete CronJob with ID: {}", id, e);
            throw new RuntimeException("Failed to delete CronJob: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsById(Long id) {
        return cronJobRepository.existsById(id);
    }

    @Override
    public CronJobModel runNow(Long id) {
        LOG.info("Manual execution requested for CronJob ID: {}", id);

        // This transaction should be short to avoid blocking other operations
        ManualExecutionContext executionContext = transactionTemplate.execute(new TransactionCallback<ManualExecutionContext>() {
            @Override
            public ManualExecutionContext doInTransaction(TransactionStatus status) {
                // This ensures only one execution (manual or scheduled) can run at a time
                CronJobModel cronJobModel = cronJobRepository.findByIdWithLock(id)
                        .orElseThrow(() -> new RuntimeException("CronJob not found with id: " + id));

                if (!cronJobModel.getEnabled()) {
                    LOG.warn("CronJob {} is disabled, cannot run manually", cronJobModel.getCode());
                    throw new RuntimeException("CronJob is disabled");
                }

                // With pessimistic lock, this check is atomic and prevents race conditions
                if (cronJobModel.getStatus() == CronJobStatus.RUNNING) {
                    LOG.warn("CronJob {} is already running, cannot start another execution", cronJobModel.getCode());
                    throw new JobAlreadyRunningException("CronJob is already running. Please wait for the current execution to complete.");
                }

                // Also check for running executions in database
                if (jobExecutionService.hasRunningExecution(id)) {
                    LOG.warn("CronJob {} has running executions, cannot start another execution", cronJobModel.getCode());
                    throw new JobAlreadyRunningException("CronJob has running executions. Please wait for the current execution to complete.");
                }

                // Immediately set status to RUNNING to prevent other executions
                // This is done while holding the pessimistic lock
                cronJobModel.setStatus(CronJobStatus.RUNNING);
                cronJobModel.setLastStartTime(OffsetDateTime.now());
                cronJobModel = cronJobRepository.saveAndFlush(cronJobModel);

                LOG.info("Starting manual execution of CronJob: {} (ID: {})", cronJobModel.getCode(), id);

                // Create execution record
                JobExecutionModel execution = new JobExecutionModel();
                execution.setJobDefinition(cronJobModel);
                execution.setStatus(JobExecutionModel.Status.RUNNING);
                execution.setStartedAt(OffsetDateTime.now());
                execution.setLogLevel(cronJobModel.getLogLevel().name());
                execution.setNodeId(nodeIdentifier.getNodeId());
                String correlationId = "MANUAL-" + System.currentTimeMillis();
                execution.setCorrelationId(correlationId);
                execution = jobExecutionService.save(execution);

                // Flush to ensure execution is immediately visible in database
                entityManager.flush();

                // Record metrics: execution start
                jobMetricsService.recordExecutionStart(execution.getId(), cronJobModel);
                jobMetricsService.updateJobStatus(null, CronJobStatus.RUNNING);

                // Start log collection with correlation ID
                Level jobLogLevel = ch.qos.logback.classic.Level.toLevel(cronJobModel.getLogLevel().name(), ch.qos.logback.classic.Level.INFO);
                jobLogCollector.startLogCollection(execution.getId(), correlationId, jobLogLevel);

                return new ManualExecutionContext(cronJobModel, execution, correlationId);
            }
        });

        try {
            var result = jobRegistry.executeJob(executionContext.cronJobModel, executionContext.execution.getId());

            return transactionTemplate.execute(new TransactionCallback<CronJobModel>() {
                @Override
                public CronJobModel doInTransaction(TransactionStatus status) {
                    jobLogCollector.stopLogCollectionAndPersist(executionContext.execution.getId(), executionContext.execution);

                    CronJobModel cronJobModel = cronJobRepository.findById(executionContext.cronJobModel.getId())
                            .orElseThrow(() -> new RuntimeException("CronJob not found: " + executionContext.cronJobModel.getId()));
                    JobExecutionModel execution = jobExecutionService.findById(executionContext.execution.getId())
                            .orElseThrow(() -> new RuntimeException("Execution not found: " + executionContext.execution.getId()));

                    if (executionContext.execution.getLogs() != null && !executionContext.execution.getLogs().isEmpty()) {
                        if (execution.getLogs() == null || execution.getLogs().isEmpty()) {
                            execution.setLogs(new ArrayList<>(executionContext.execution.getLogs()));
                        }
                    }

                    boolean isCancelled = execution.getStatus() == JobExecutionModel.Status.CANCELLED;
                    boolean isAbortedFromResult = result != null && !result.getSuccess() &&
                            result.getMessage() != null &&
                            (result.getMessage().toLowerCase().contains("aborted") ||
                                    result.getMessage().toLowerCase().contains("cancelled"));

                    CronJobStatus oldStatus = cronJobModel.getStatus();
                    CronJobStatus newStatus = null;

                    if (isCancelled || isAbortedFromResult) {
                        execution.setStatus(JobExecutionModel.Status.CANCELLED);
                        execution.setEndedAt(OffsetDateTime.now());

                        cronJobModel.setStatus(CronJobStatus.CANCELLED);
                        cronJobModel.setLastEndTime(OffsetDateTime.now());
                        cronJobModel.setLastResult("CANCELLED: " + (result != null ? result.getMessage() : "Job aborted by user"));

                        jobLogCollector.addLog(execution.getId(), "WARN", "Job was cancelled: " + (result != null ? result.getMessage() : "Job aborted by user"));
                        jobLogCollector.addLog(execution.getId(), "INFO", "Execution statistics: " + jobLogCollector.getStatistics());
                        jobLogCollector.stopLogCollectionAndPersist(execution.getId(), execution);

                        jobExecutionService.save(execution);
                        newStatus = CronJobStatus.CANCELLED;
                    } else if (result.getSuccess()) {
                        execution.setStatus(JobExecutionModel.Status.SUCCESS);
                        execution.setEndedAt(OffsetDateTime.now());

                        cronJobModel.setStatus(CronJobStatus.FINISHED);
                        cronJobModel.setLastEndTime(OffsetDateTime.now());
                        cronJobModel.setLastResult("SUCCESS: " + result.getMessage());

                        jobLogCollector.addLog(execution.getId(), "INFO", "Job completed successfully: " + result.getMessage());
                        jobLogCollector.stopLogCollectionAndPersist(execution.getId(), execution);

                        jobExecutionService.save(execution);
                        newStatus = CronJobStatus.FINISHED;
                    } else {
                        execution.setStatus(JobExecutionModel.Status.FAILED);
                        execution.setEndedAt(OffsetDateTime.now());

                        cronJobModel.setStatus(CronJobStatus.FAILED);
                        cronJobModel.setLastEndTime(OffsetDateTime.now());
                        cronJobModel.setLastResult("FAILED: " + result.getMessage());
                        cronJobModel.setRetryCount(cronJobModel.getRetryCount() + 1);

                        jobLogCollector.addLog(execution.getId(), "ERROR", "Job failed: " + result.getMessage());
                        jobLogCollector.stopLogCollectionAndPersist(execution.getId(), execution);

                        jobExecutionService.save(execution);
                        newStatus = CronJobStatus.FAILED;
                    }

                    // Record metrics: execution complete and status change
                    jobMetricsService.recordExecutionComplete(execution.getId(), cronJobModel, execution);
                    jobMetricsService.updateJobStatus(oldStatus, newStatus);

                    try {
                        return cronJobRepository.saveAndFlush(cronJobModel);
                    } catch (OptimisticLockingFailureException e) {
                        // If optimistic locking fails, reload and retry
                        CronJobModel freshCronJob = cronJobRepository.findById(cronJobModel.getId()).orElse(null);
                        if (freshCronJob != null) {
                            freshCronJob.setStatus(cronJobModel.getStatus());
                            freshCronJob.setLastResult(cronJobModel.getLastResult());
                            freshCronJob.setRetryCount(cronJobModel.getRetryCount());
                            freshCronJob.setLastStartTime(cronJobModel.getLastStartTime());
                            freshCronJob.setLastEndTime(cronJobModel.getLastEndTime());
                            return cronJobRepository.saveAndFlush(freshCronJob);
                        }
                        return cronJobModel;
                    }
                }
            });
        } catch (Exception e) {
            return transactionTemplate.execute(new TransactionCallback<CronJobModel>() {
                @Override
                public CronJobModel doInTransaction(TransactionStatus status) {
                    jobLogCollector.stopLogCollectionAndPersist(executionContext.execution.getId(), executionContext.execution);

                    CronJobModel cronJobModel = cronJobRepository.findById(executionContext.cronJobModel.getId())
                            .orElseThrow(() -> new RuntimeException("CronJob not found: " + executionContext.cronJobModel.getId()));
                    JobExecutionModel execution = jobExecutionService.findById(executionContext.execution.getId())
                            .orElseThrow(() -> new RuntimeException("Execution not found: " + executionContext.execution.getId()));

                    if (executionContext.execution.getLogs() != null && !executionContext.execution.getLogs().isEmpty()) {
                        if (execution.getLogs() == null || execution.getLogs().isEmpty()) {
                            execution.setLogs(new ArrayList<>(executionContext.execution.getLogs()));
                        }
                    }

                    CronJobStatus oldStatus = cronJobModel.getStatus();
                    CronJobStatus newStatus = null;

                    // Check if execution was cancelled/aborted before handling exception
                    boolean isCancelled = execution.getStatus() == JobExecutionModel.Status.CANCELLED;

                    if (isCancelled) {
                        // Execution was already cancelled (aborted), keep it as CANCELLED
                        execution.setEndedAt(OffsetDateTime.now());

                        cronJobModel.setStatus(CronJobStatus.CANCELLED);
                        cronJobModel.setLastEndTime(OffsetDateTime.now());
                        cronJobModel.setLastResult("CANCELLED: Job aborted by user");

                        jobLogCollector.addLog(execution.getId(), "WARN", "Job was cancelled: Job aborted by user");
                        jobLogCollector.addLog(execution.getId(), "INFO", "Execution statistics: " + jobLogCollector.getStatistics());
                        jobLogCollector.stopLogCollectionAndPersist(execution.getId(), execution);

                        jobExecutionService.save(execution);
                        newStatus = CronJobStatus.CANCELLED;
                    } else {
                        // Real exception occurred, mark as FAILED
                        execution.setStatus(JobExecutionModel.Status.FAILED);
                        execution.setEndedAt(OffsetDateTime.now());

                        cronJobModel.setStatus(CronJobStatus.FAILED);
                        cronJobModel.setLastEndTime(OffsetDateTime.now());
                        cronJobModel.setLastResult("EXCEPTION: " + e.getMessage());
                        cronJobModel.setRetryCount(cronJobModel.getRetryCount() + 1);

                        jobLogCollector.addLog(execution.getId(), "ERROR", "Job failed with exception: " + e.getMessage());
                        // stopLogCollectionAndPersist already called above, but call again to ensure final logs are included
                        jobLogCollector.stopLogCollectionAndPersist(execution.getId(), execution);

                        jobExecutionService.save(execution);
                        newStatus = CronJobStatus.FAILED;
                    }

                    // Record metrics: execution complete and status change
                    jobMetricsService.recordExecutionComplete(execution.getId(), cronJobModel, execution);
                    jobMetricsService.updateJobStatus(oldStatus, newStatus);

                    try {
                        return cronJobRepository.saveAndFlush(cronJobModel);
                    } catch (OptimisticLockingFailureException ex) {
                        // If optimistic locking fails, reload and retry
                        CronJobModel freshCronJob = cronJobRepository.findById(cronJobModel.getId()).orElse(null);
                        if (freshCronJob != null) {
                            freshCronJob.setStatus(newStatus);
                            freshCronJob.setLastEndTime(OffsetDateTime.now());
                            freshCronJob.setLastResult(isCancelled ? "CANCELLED: Job aborted by user" : "EXCEPTION: " + e.getMessage());
                            if (!isCancelled) {
                                freshCronJob.setRetryCount(freshCronJob.getRetryCount() + 1);
                            }
                            return cronJobRepository.saveAndFlush(freshCronJob);
                        }
                        return cronJobModel;
                    }
                }
            });
        }
    }

    private record ManualExecutionContext(CronJobModel cronJobModel, JobExecutionModel execution,
                                          String correlationId) {
    }

    @Override
    public List<String> getAvailableJobs() {
        return List.of("simpleTestJob", "productIndexingJob");
    }
}

