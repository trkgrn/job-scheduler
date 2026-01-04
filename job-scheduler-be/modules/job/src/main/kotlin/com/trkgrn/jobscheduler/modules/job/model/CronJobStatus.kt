package com.trkgrn.jobscheduler.modules.job.model

/**
 * CronJob status enumeration
 */
enum class CronJobStatus {
    /**
     * Unknown status (initial state)
     */
    UNKNOWN,
    
    /**
     * Job is currently running
     */
    RUNNING,
    
    /**
     * Job completed successfully
     */
    FINISHED,
    
    /**
     * Job failed
     */
    FAILED,
    
    /**
     * Job was cancelled/aborted
     */
    CANCELLED,
    
    /**
     * Job is paused
     */
    PAUSED
}

