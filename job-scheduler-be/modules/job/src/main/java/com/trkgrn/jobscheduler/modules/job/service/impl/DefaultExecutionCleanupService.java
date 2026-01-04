package com.trkgrn.jobscheduler.modules.job.service.impl;

import com.trkgrn.jobscheduler.modules.job.model.CronJobModel;
import com.trkgrn.jobscheduler.modules.job.model.CronJobStatus;
import com.trkgrn.jobscheduler.modules.job.model.JobExecutionModel;
import com.trkgrn.jobscheduler.modules.job.repository.CronJobRepository;
import com.trkgrn.jobscheduler.modules.job.repository.JobExecutionRepository;
import com.trkgrn.jobscheduler.modules.job.service.ExecutionCleanupService;
import com.trkgrn.jobscheduler.modules.job.util.NodeIdentifier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service to clean up stuck/zombie executions on application startup
 * Handles cases where application was stopped while jobs were running
 */
@Service
public class DefaultExecutionCleanupService implements ExecutionCleanupService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultExecutionCleanupService.class);
    
    @Value("${job.execution.timeout-minutes:60}")
    private long executionTimeoutMinutes;
    
    private final JobExecutionRepository jobExecutionRepository;
    private final CronJobRepository cronJobRepository;
    private final NodeIdentifier nodeIdentifier;
    private final EntityManager entityManager;

    public DefaultExecutionCleanupService(JobExecutionRepository jobExecutionRepository,
                                         CronJobRepository cronJobRepository,
                                         NodeIdentifier nodeIdentifier,
                                         EntityManager entityManager) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.cronJobRepository = cronJobRepository;
        this.nodeIdentifier = nodeIdentifier;
        this.entityManager = entityManager;
    }

    @Override
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void cleanupStuckExecutions() {
        String currentNodeId = nodeIdentifier.getNodeId();
        LOG.info("Starting cleanup of stuck/zombie executions for node: {}", currentNodeId);
        
        try {
            // Wait a bit for database and Quartz to be fully initialized
            Thread.sleep(2000);
            
            // Get active Quartz scheduler instances (active pod/node IDs)
            Set<String> activeNodeIds = getActiveQuartzInstances();
            LOG.info("Active Quartz scheduler instances (node IDs): {}", activeNodeIds);
            
            // Find all running executions
            List<JobExecutionModel> runningExecutions = jobExecutionRepository.findByStatus(JobExecutionModel.Status.RUNNING);
            LOG.info("Found {} running executions to check", runningExecutions.size());
            
            if (runningExecutions.isEmpty()) {
                LOG.info("No stuck executions found");
                return;
            }
            
            // Filter executions that should be cleaned up:
            // 1. Execution belongs to this node (pod restart scenario - same pod, different name possible)
            // 2. Execution has no nodeId (legacy execution)
            // 3. Execution's nodeId is not in active Quartz instances (pod name changed or pod is down)
            List<JobExecutionModel> executionsToCleanup = runningExecutions.stream()
                    .filter(execution -> {
                        String executionNodeId = execution.getNodeId();
                        
                        // Clean up legacy executions
                        if (executionNodeId == null) {
                            return true;
                        }
                        
                        // Clean up executions from this node (pod restart scenario)
                        if (currentNodeId.equals(executionNodeId)) {
                            return true;
                        }
                        
                        // Clean up executions from inactive nodes (pod name changed or pod is down)
                        if (!activeNodeIds.contains(executionNodeId)) {
                            LOG.debug("Execution nodeId {} not found in active Quartz instances, will cleanup", executionNodeId);
                            return true;
                        }
                        
                        return false;
                    })
                    .collect(Collectors.toList());
            
            LOG.info("Found {} executions to cleanup out of {} total running executions", 
                    executionsToCleanup.size(), runningExecutions.size());
            
            if (executionsToCleanup.isEmpty()) {
                LOG.info("No stuck executions found");
                return;
            }
            
            int cleanedCount = 0;
            
            for (JobExecutionModel execution : executionsToCleanup) {
                try {
                    String executionNodeId = execution.getNodeId();
                    String reason = determineCleanupReason(executionNodeId, currentNodeId, activeNodeIds);
                    
                    LOG.warn("Found stuck execution: ID={}, NodeId={}, StartedAt={}, JobDefinition={}, Reason={}", 
                            execution.getId(), executionNodeId != null ? executionNodeId : "legacy",
                            execution.getStartedAt(),
                            execution.getJobDefinition() != null ? execution.getJobDefinition().getCode() : "N/A",
                            reason);
                    
                    cleanupStuckExecution(execution, reason);
                    cleanedCount++;
                } catch (Exception e) {
                    LOG.error("Error cleaning up execution ID={}: {}", execution.getId(), e.getMessage(), e);
                }
            }
            
        LOG.info("Startup cleanup completed. Cleaned {} stuck executions for node: {}", cleanedCount, currentNodeId);
        
        } catch (Exception e) {
            LOG.error("Error during execution cleanup", e);
        }
    }

    /**
     * Get active Quartz scheduler instance IDs from QRTZ_SCHEDULER_STATE table
     * These represent currently active pods/nodes
     * Quartz stores LAST_CHECKIN_TIME as BIGINT (milliseconds since epoch)
     */
    private Set<String> getActiveQuartzInstances() {
        Set<String> activeInstances = new HashSet<>();
        try {
            // Query QRTZ_SCHEDULER_STATE table to get active scheduler instances
            // LAST_CHECKIN_TIME is within last 60 seconds means instance is active
            // Quartz uses milliseconds since epoch (BIGINT)
            long currentTimeMillis = System.currentTimeMillis();
            long thresholdTime = currentTimeMillis - 60000; // 60 seconds ago
            
            Query query = entityManager.createNativeQuery(
                    "SELECT INSTANCE_NAME FROM QRTZ_SCHEDULER_STATE " +
                    "WHERE LAST_CHECKIN_TIME > :thresholdTime"
            );
            query.setParameter("thresholdTime", thresholdTime);
            
            @SuppressWarnings("unchecked")
            List<String> instances = query.getResultList();
            activeInstances.addAll(instances);
            
            LOG.debug("Found {} active Quartz scheduler instances: {}", activeInstances.size(), activeInstances);
        } catch (Exception e) {
            LOG.warn("Could not query Quartz scheduler state, will use fallback logic: {}", e.getMessage());
            // Fallback: if we can't query Quartz state, assume only current node is active
            activeInstances.add(nodeIdentifier.getNodeId());
        }
        return activeInstances;
    }

    private String determineCleanupReason(String executionNodeId, String currentNodeId, Set<String> activeNodeIds) {
        if (executionNodeId == null) {
            return "Legacy execution (no nodeId) - Application restart on node: " + currentNodeId;
        }
        
        if (currentNodeId.equals(executionNodeId)) {
            return "Application restart on node: " + currentNodeId + " (same node, pod may have restarted)";
        }
        
        if (!activeNodeIds.contains(executionNodeId)) {
            return "Execution from inactive node: " + executionNodeId + 
                   " (pod name changed or pod is down) - Current node: " + currentNodeId;
        }
        
        return "Application restart on node: " + currentNodeId;
    }

    /**
     * Periodic cleanup of stuck executions (runs every 30 minutes)
     * Checks for executions that have been running longer than the configured timeout
     * Only cleans up executions belonging to this node (or legacy executions without nodeId)
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes
    @Transactional
    public void cleanupStuckExecutionsPeriodically() {
        String currentNodeId = nodeIdentifier.getNodeId();
        LOG.debug("Starting periodic cleanup of stuck executions for node: {}", currentNodeId);
        
        try {
            // Find all running executions
            List<JobExecutionModel> runningExecutions = jobExecutionRepository.findByStatus(JobExecutionModel.Status.RUNNING);
            
            if (runningExecutions.isEmpty()) {
                return;
            }
            
            OffsetDateTime timeoutThreshold = OffsetDateTime.now().minusMinutes(executionTimeoutMinutes);
            
            // Filter executions that belong to this node (or have no nodeId set - legacy executions)
            List<JobExecutionModel> executionsToCheck = runningExecutions.stream()
                    .filter(execution -> {
                        String executionNodeId = execution.getNodeId();
                        return currentNodeId.equals(executionNodeId) || executionNodeId == null;
                    })
                    .collect(Collectors.toList());
            
            if (executionsToCheck.isEmpty()) {
                return;
            }
            
            int cleanedCount = 0;
            
            for (JobExecutionModel execution : executionsToCheck) {
                try {
                    // Check if execution has exceeded timeout
                    if (execution.getStartedAt() != null && execution.getStartedAt().isBefore(timeoutThreshold)) {
                        LOG.warn("Found stuck execution exceeding timeout ({} minutes): ID={}, NodeId={}, StartedAt={}, JobDefinition={}", 
                                executionTimeoutMinutes, execution.getId(), 
                                execution.getNodeId() != null ? execution.getNodeId() : "legacy",
                                execution.getStartedAt(),
                                execution.getJobDefinition() != null ? execution.getJobDefinition().getCode() : "N/A");
                        
                        cleanupStuckExecution(execution, "Execution timeout exceeded (" + executionTimeoutMinutes + " minutes) on node: " + currentNodeId);
                        cleanedCount++;
                    }
                } catch (Exception e) {
                    LOG.error("Error cleaning up execution ID={}: {}", execution.getId(), e.getMessage(), e);
                }
            }
            
            if (cleanedCount > 0) {
                LOG.info("Periodic cleanup completed. Cleaned {} stuck executions for node: {}", cleanedCount, currentNodeId);
            }
            
        } catch (Exception e) {
            LOG.error("Error during periodic execution cleanup", e);
        }
    }

    private void cleanupStuckExecution(JobExecutionModel execution, String reason) {
        OffsetDateTime now = OffsetDateTime.now();
        
        // Mark execution as FAILED
        execution.setStatus(JobExecutionModel.Status.FAILED);
        execution.setEndedAt(now);
        
        // Add log entry explaining why it was marked as failed
        List<JobExecutionModel.LogEntry> logs = execution.getLogs();
        if (logs == null) {
            logs = new ArrayList<>();
        }
        logs.add(new JobExecutionModel.LogEntry(
                now.toString(),
                "ERROR",
                "Execution marked as FAILED: " + reason
        ));
        execution.setLogs(logs);
        
        jobExecutionRepository.save(execution);
        
        // Update associated CronJob status if exists
        if (execution.getJobDefinition() != null) {
            CronJobModel cronJob = execution.getJobDefinition();
            
            // Only update if still in RUNNING status (might have been updated by another process)
            if (cronJob.getStatus() == CronJobStatus.RUNNING) {
                cronJob.setStatus(CronJobStatus.FAILED);
                cronJob.setLastEndTime(now);
                cronJob.setLastResult("FAILED: " + reason);
                cronJobRepository.save(cronJob);
                
                LOG.info("Updated CronJob {} status from RUNNING to FAILED", cronJob.getCode());
            }
        }
        
        LOG.info("Cleaned up stuck execution: ID={}", execution.getId());
    }
}

