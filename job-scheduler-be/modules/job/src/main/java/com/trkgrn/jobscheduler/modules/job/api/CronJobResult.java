package com.trkgrn.jobscheduler.modules.job.api;

/**
 * CronJob execution result type
 */
public enum CronJobResult {
    /**
     * Job completed successfully
     */
    SUCCESS,
    
    /**
     * Job failed or encountered an error
     */
    ERROR
}

