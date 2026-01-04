package com.trkgrn.jobscheduler.modules.job.api;

import com.trkgrn.jobscheduler.modules.job.model.CronJobStatus;

import java.io.Serializable;

/**
 * Job execution result
 */
public class PerformResult implements Serializable {
    
    private final CronJobResult result;
    private final CronJobStatus status;
    private final String message;
    private final Object data;
    private final Throwable error;
    
    /**
     * Create PerformResult with result and status
     */
    public PerformResult(CronJobResult result, CronJobStatus status) {
        this(result, status, null, null, null);
    }
    
    /**
     * Create PerformResult with result, status and message
     */
    public PerformResult(CronJobResult result, CronJobStatus status, String message) {
        this(result, status, message, null, null);
    }
    
    /**
     * Create PerformResult with all fields
     */
    public PerformResult(CronJobResult result, CronJobStatus status, String message, Object data, Throwable error) {
        this.result = result;
        this.status = status;
        this.message = message;
        this.data = data;
        this.error = error;
    }
    
    public CronJobResult getResult() {
        return result;
    }
    
    public CronJobStatus getStatus() {
        return status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Object getData() {
        return data;
    }
    
    public Throwable getError() {
        return error;
    }
    
    /**
     * Check if result is success
     */
    public boolean isSuccess() {
        return result == CronJobResult.SUCCESS;
    }
    
    /**
     * Convert to JobResult for backward compatibility
     */
    public JobResult toJobResult() {
        return new JobResult(
            isSuccess(),
            message,
            data,
            error
        );
    }
    
    /**
     * Static factory methods for convenience
     */
    public static PerformResult success(CronJobStatus status) {
        return new PerformResult(CronJobResult.SUCCESS, status);
    }
    
    public static PerformResult success(CronJobStatus status, String message) {
        return new PerformResult(CronJobResult.SUCCESS, status, message);
    }
    
    public static PerformResult error(CronJobStatus status, String message) {
        return new PerformResult(CronJobResult.ERROR, status, message);
    }
    
    public static PerformResult error(CronJobStatus status, String message, Throwable error) {
        return new PerformResult(CronJobResult.ERROR, status, message, null, error);
    }
    
    /**
     * Abort result (convenience method)
     */
    public static PerformResult aborted(String message) {
        return new PerformResult(CronJobResult.ERROR, CronJobStatus.CANCELLED, message);
    }
}

