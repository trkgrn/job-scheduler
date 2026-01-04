package com.trkgrn.jobscheduler.modules.job.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark job components with metadata
 * Used for job discovery and dynamic form generation
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JobComponent {
    
    /**
     * Display name for the job (shown in UI)
     */
    String displayName();
    
    /**
     * Description of what the job does
     */
    String description();
    
    /**
     * Category for grouping jobs (e.g., SEARCH, NOTIFICATION, CLEANUP)
     */
    String category() default "GENERAL";
    
    /**
     * Parameters that this job accepts
     */
    JobParameter[] parameters() default {};
}

