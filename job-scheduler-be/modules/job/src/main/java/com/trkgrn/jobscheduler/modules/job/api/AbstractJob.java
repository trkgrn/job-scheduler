package com.trkgrn.jobscheduler.modules.job.api;

import com.trkgrn.jobscheduler.modules.job.model.CronJobModel;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for all job implementations
 * Provides default implementations for cancellation-related methods
 * 
 * Usage for non-abortable jobs:
 * <pre>
 * public class MyJob extends AbstractJob&lt;CronJobModel&gt; {
 *     {@literal @}Override
 *     public String getJobName() {
 *         return "My Job";
 *     }
 *     
 *     {@literal @}Override
 *     public JobResult execute(CronJobModel cronJob) {
 *         // Your job logic here
 *         return new JobResult(true, "Job completed");
 *     }
 * }
 * </pre>
 * 
 * For abortable jobs, extend {@link AbortableJob} instead (which extends this class)
 */
public abstract class AbstractJob<T extends CronJobModel> implements Job<T> {
    
    /**
     * Abstract method - must be implemented by subclasses
     * @return Unique job name/identifier
     */
    @NotNull
    public abstract String getJobName();
    
    /**
     * Abstract method - must be implemented by subclasses
     * @param cronJobModel The CronJobModel instance containing job configuration
     * @return Job execution result
     */
    @NotNull
    public abstract JobResult execute(@NotNull T cronJobModel);
    
    @Override
    public boolean isAbortable() {
        return false;
    }
    
    @Override
    public void setExecutionContext(JobExecutionContext context) {
        // Default: no-op for non-abortable jobs
    }
    
    @Override
    public boolean isCancellationRequested(Long executionId) {
        // Default: always return false for non-abortable jobs
        return false;
    }
}

