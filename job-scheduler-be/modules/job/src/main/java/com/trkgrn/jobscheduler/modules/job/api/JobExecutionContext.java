package com.trkgrn.jobscheduler.modules.job.api;

import com.trkgrn.jobscheduler.modules.job.model.JobExecutionModel;
import com.trkgrn.jobscheduler.modules.job.repository.JobExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Execution context for job cancellation support
 * 
 * Provides:
 * - In-memory cancellation flag for fast checks (nanoseconds)
 * - Periodic database sync for multi-pod safety
 * - Thread-safe cancellation tracking
 */
@Component
public class JobExecutionContext {
    
    private static final Logger LOG = LoggerFactory.getLogger(JobExecutionContext.class);
    
    // In-memory cancellation flags (executionId -> cancelled)
    private static final ConcurrentHashMap<Long, Boolean> cancellationFlags = new ConcurrentHashMap<>();
    
    // Check counters per execution (for periodic DB checks)
    private static final ConcurrentHashMap<Long, AtomicInteger> checkCounters = new ConcurrentHashMap<>();
    
    // Default check interval: every 50 steps
    private static final int DEFAULT_CHECK_INTERVAL = 50;
    
    private final JobExecutionRepository jobExecutionRepository;
    
    public JobExecutionContext(JobExecutionRepository jobExecutionRepository) {
        this.jobExecutionRepository = jobExecutionRepository;
    }
    
    /**
     * Initialize execution context for a job execution
     * Should be called at the start of job execution
     */
    public void initialize(Long executionId) {
        // Load cancellation status from DB
        boolean cancelled = jobExecutionRepository.findById(executionId)
                .map(execution -> execution.getStatus() == JobExecutionModel.Status.CANCELLED)
                .orElse(false);
        
        cancellationFlags.put(executionId, cancelled);
        checkCounters.put(executionId, new AtomicInteger(0));
        
        LOG.debug("Initialized execution context for execution ID: {} (cancelled: {})", executionId, cancelled);
    }
    
    /**
     * Check if execution is cancelled
     * Uses hybrid approach: in-memory flag + periodic DB sync
     * 
     * @param executionId Execution ID
     * @return true if cancelled, false otherwise
     */
    public boolean isCancelled(Long executionId) {
        // Fast path: check in-memory flag
        Boolean cancelled = cancellationFlags.get(executionId);
        if (cancelled == null) {
            // Execution context not initialized, check DB
            return checkCancellationFromDB(executionId);
        }
        
        // Periodic DB check (every N steps) for multi-pod safety
        AtomicInteger counter = checkCounters.get(executionId);
        if (counter != null && counter.incrementAndGet() % DEFAULT_CHECK_INTERVAL == 0) {
            boolean dbCancelled = checkCancellationFromDB(executionId);
            if (dbCancelled != cancelled) {
                // Status changed, update in-memory flag
                cancellationFlags.put(executionId, dbCancelled);
                LOG.info("Cancellation status changed for execution ID: {} (now: {})", executionId, dbCancelled);
                return dbCancelled;
            }
        }
        
        return cancelled;
    }
    
    /**
     * Request cancellation for an execution
     * Updates both DB and in-memory flag
     */
    public void requestCancellation(Long executionId) {
        cancellationFlags.put(executionId, true);
        LOG.info("Cancellation requested for execution ID: {}", executionId);
    }
    
    /**
     * Clear execution context after job completion
     */
    public void clear(Long executionId) {
        cancellationFlags.remove(executionId);
        checkCounters.remove(executionId);
        LOG.debug("Cleared execution context for execution ID: {}", executionId);
    }
    
    /**
     * Check cancellation status from database
     * Used for periodic sync and initialization
     */
    private boolean checkCancellationFromDB(Long executionId) {
        return jobExecutionRepository.findById(executionId)
                .map(execution -> execution.getStatus() == JobExecutionModel.Status.CANCELLED)
                .orElse(false);
    }
    
    /**
     * Get check interval (for custom implementations)
     */
    public int getCheckInterval() {
        return DEFAULT_CHECK_INTERVAL;
    }
}

