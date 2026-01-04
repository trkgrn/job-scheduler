package com.trkgrn.jobscheduler.modules.job.annotation;

/**
 * Enumeration of supported parameter types for job parameters
 */
public enum ParameterType {
    
    /**
     * String parameter
     */
    STRING,
    
    /**
     * Integer parameter
     */
    INTEGER,
    
    /**
     * Boolean parameter
     */
    BOOLEAN,
    
    /**
     * JSON object parameter
     */
    JSON,
    
    /**
     * Enumeration parameter (dropdown)
     */
    ENUM,
    
    /**
     * Date parameter
     */
    DATE,
    
    /**
     * Long text parameter (textarea)
     */
    TEXTAREA
}

