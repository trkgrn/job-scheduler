package com.trkgrn.jobscheduler.modules.job.api;

import com.trkgrn.jobscheduler.modules.job.model.CronJobModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for abortable jobs
 * Extends AbstractJob and provides cancellation mechanism
 */
public abstract class AbortableJob<T extends CronJobModel> extends AbstractJob<T> {
    
    private static final Logger LOG = LoggerFactory.getLogger(AbortableJob.class);
    
    private JobExecutionContext executionContext;
    private Long currentExecutionId;
    
    @Override
    public boolean isAbortable() {
        return true;
    }
    
    @Override
    public void setExecutionContext(JobExecutionContext context) {
        this.executionContext = context;
    }
    
    /**
     * Set current execution ID
     * Called by JobRegistry before execution
     */
    public void setCurrentExecutionId(Long executionId) {
        this.currentExecutionId = executionId;
        if (executionContext != null && executionId != null) {
            executionContext.initialize(executionId);
        }
    }
    
    /**
     * Check if cancellation is requested (default implementation)
     * Uses JobExecutionContext for fast in-memory checks + periodic DB sync
     * 
     * Override isCancellationRequested() for custom logic
     * 
     * @return true if cancellation is requested, false otherwise
     */
    protected boolean checkCancellation() {
        if (currentExecutionId == null) {
            return false;
        }
        return isCancellationRequested(currentExecutionId);
    }
    
    /**
     * Check and clear abort request if needed
     * 
     * @param cronJob CronJobModel instance
     * @return true if abort was requested, false otherwise
     */
    protected boolean clearAbortRequestedIfNeeded(T cronJob) {
        if (currentExecutionId == null) {
            return false;
        }
        
        boolean cancelled = isCancellationRequested(currentExecutionId);
        if (cancelled) {
            LOG.info("Abort requested detected for execution ID: {} (CronJob: {})", 
                    currentExecutionId, cronJob.getCode());
            // Note: We don't clear the flag here as it's managed by JobExecutionContext
            // The flag will be cleared when execution completes
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean isCancellationRequested(Long executionId) {
        if (executionContext == null || executionId == null) {
            return false;
        }
        return executionContext.isCancelled(executionId);
    }
    
    /**
     * Cleanup execution context after job completion
     * Should be called in finally block or at the end of execute()
     */
    protected void cleanup() {
        if (executionContext != null && currentExecutionId != null) {
            executionContext.clear(currentExecutionId);
        }
    }
    
    /**
     * Get execution context (for advanced usage)
     */
    protected JobExecutionContext getExecutionContext() {
        return executionContext;
    }
    
    /**
     * Get current execution ID (for advanced usage)
     */
    protected Long getCurrentExecutionId() {
        return currentExecutionId;
    }
}

