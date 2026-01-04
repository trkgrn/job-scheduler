package com.trkgrn.jobscheduler.modules.job.api

import java.io.Serializable

/**
 * Abstract job interface that all job implementations must extend.
 */
interface Job<T : Any> : Serializable {

    /**
     * Unique job name/identifier
     */
    fun getJobName(): String

    /**
     * Execute the job with CronJobModel parameter
     * @param cronJobModel The specific CronJobModel instance containing job configuration
     * @return Job execution result
     */
    fun execute(cronJobModel: T): JobResult

    /**
     * Optional: Job description for UI/admin purposes
     */
    fun getDescription(): String = ""

    /**
     * Optional: Validate CronJobModel before execution
     */
    fun validateCronJobModel(cronJobModel: T): Boolean = true
    
    /**
     * Check if this job supports cancellation/abort
     * 
     * @return true if job can be cancelled, false otherwise
     */
    fun isAbortable(): Boolean = false
    
    /**
     * Check if cancellation is requested for this execution
     * Job implementations should call this periodically in their loops
     * 
     * Default implementation: Uses JobExecutionContext (if set)
     * Override this method for custom cancellation logic
     * 
     * @param executionId Execution ID
     * @return true if cancellation is requested, false otherwise
     */
    fun isCancellationRequested(executionId: Long?): Boolean = false
    
    /**
     * Set execution context for cancellation support
     * Called by JobRegistry before job execution
     * 
     * @param context JobExecutionContext instance
     */
    fun setExecutionContext(context: JobExecutionContext?) {
        // Default: no-op, can be overridden
    }
}

/**
 * Job execution result
 */
data class JobResult @JvmOverloads constructor(
    val success: Boolean,
    val message: String? = null,
    val data: Any? = null,
    val error: Throwable? = null
) : Serializable

