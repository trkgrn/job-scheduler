package com.trkgrn.jobscheduler.modules.job.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define job parameters
 * Used within @JobComponent to specify job parameters
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JobParameter {
    
    /**
     * Parameter name (must match field name in job model)
     */
    String name();
    
    /**
     * Parameter type for UI rendering
     */
    ParameterType type();
    
    /**
     * Display name for the parameter (shown in UI)
     */
    String displayName();
    
    /**
     * Description of the parameter
     */
    String description() default "";
    
    /**
     * Whether this parameter is required
     */
    boolean required() default false;
    
    /**
     * Default value as string (will be converted based on type)
     */
    String defaultValue() default "";
    
    /**
     * Validation rules (e.g., "min:1,max:1000" for integers)
     */
    String validation() default "";
    
    /**
     * For ENUM type: comma-separated options
     */
    String options() default "";
}

