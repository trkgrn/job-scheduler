package com.trkgrn.jobscheduler.modules.job.validator;

import com.trkgrn.jobscheduler.modules.job.dto.JobMetadataDto;
import com.trkgrn.jobscheduler.modules.job.dto.JobParameterDto;
import com.trkgrn.jobscheduler.modules.job.registry.EnhancedJobRegistry;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for validating job parameters
 */
@Service
public class JobParameterValidator {
    
    private final EnhancedJobRegistry enhancedJobRegistry;
    
    public JobParameterValidator(EnhancedJobRegistry enhancedJobRegistry) {
        this.enhancedJobRegistry = enhancedJobRegistry;
    }
    
    /**
     * Validate job parameters against job metadata
     */
    public ValidationResult validateParameters(String beanName, Map<String, Object> parameters) {
        if (beanName == null || beanName.trim().isEmpty()) {
            return new ValidationResult(false, "Bean name is required");
        }
        
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        
        Optional<JobMetadataDto> metadataOpt = enhancedJobRegistry.getJobMetadata(beanName);
        
        if (metadataOpt.isEmpty()) {
            return new ValidationResult(false, "Job not found: " + beanName);
        }
        
        JobMetadataDto metadata = metadataOpt.get();
        List<JobParameterDto> parameterDefinitions = metadata.getParameters();
        
        if (parameterDefinitions == null) {
            parameterDefinitions = new ArrayList<>();
        }
        
        List<String> errors = new ArrayList<>();
        
        // Check required parameters
        for (JobParameterDto param : parameterDefinitions) {
            if (param == null) {
                continue;
            }
            
            String paramName = param.getName();
            if (paramName == null || paramName.trim().isEmpty()) {
                continue;
            }
            
            if (Boolean.TRUE.equals(param.getRequired()) && !parameters.containsKey(paramName)) {
                String displayName = param.getDisplayName();
                errors.add("Required parameter missing: " + (displayName != null ? displayName : paramName));
            }
        }
        
        // Validate parameter types and values
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String paramName = entry.getKey();
            Object paramValue = entry.getValue();
            
            if (paramName == null) {
                continue;
            }
            
            JobParameterDto paramDef = findParameterDefinition(parameterDefinitions, paramName);
            if (paramDef != null) {
                String validationError = validateParameterValue(paramDef, paramValue);
                if (validationError != null) {
                    errors.add(validationError);
                }
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    private JobParameterDto findParameterDefinition(List<JobParameterDto> parameters, String name) {
        if (parameters == null || name == null) {
            return null;
        }
        
        return parameters.stream()
                .filter(param -> param != null && name.equals(param.getName()))
                .findFirst()
                .orElse(null);
    }
    
    private String validateParameterValue(JobParameterDto paramDef, Object value) {
        if (paramDef == null) {
            return null;
        }
        
        String displayName = paramDef.getDisplayName();
        String paramDisplayName = displayName != null ? displayName : "Unknown";
        
        if (value == null) {
            return Boolean.TRUE.equals(paramDef.getRequired()) ? 
                "Parameter " + paramDisplayName + " is required" : null;
        }
        
        // Type validation
        String paramType = paramDef.getType();
        if (paramType != null) {
            switch (paramType) {
                case "INTEGER":
                    if (!(value instanceof Integer)) {
                        return "Parameter " + paramDisplayName + " must be an integer";
                    }
                    break;
                case "BOOLEAN":
                    if (!(value instanceof Boolean)) {
                        return "Parameter " + paramDisplayName + " must be a boolean";
                    }
                    break;
                case "STRING":
                    if (!(value instanceof String)) {
                        return "Parameter " + paramDisplayName + " must be a string";
                    }
                    break;
            }
        }
        
        // Custom validation rules
        String validation = paramDef.getValidation();
        if (validation != null && !validation.trim().isEmpty()) {
            return validateCustomRules(paramDef, value);
        }
        
        return null;
    }
    
    private String validateCustomRules(JobParameterDto paramDef, Object value) {
        if (paramDef == null || value == null) {
            return null;
        }
        
        String validation = paramDef.getValidation();
        if (validation == null || validation.trim().isEmpty()) {
            return null;
        }
        
        String displayName = paramDef.getDisplayName();
        String paramDisplayName = displayName != null ? displayName : "Unknown";
        
        if (validation.contains("min:") && validation.contains("max:")) {
            try {
                // Extract min and max values
                String[] parts = validation.split(",");
                if (parts.length >= 2) {
                    String minPart = parts[0].trim();
                    String maxPart = parts[1].trim();
                    
                    if (minPart.contains(":") && maxPart.contains(":")) {
                        int min = Integer.parseInt(minPart.split(":")[1].trim());
                        int max = Integer.parseInt(maxPart.split(":")[1].trim());
                        
                        if (value instanceof Integer) {
                            int intValue = (Integer) value;
                            if (intValue < min || intValue > max) {
                                return "Parameter " + paramDisplayName + 
                                       " must be between " + min + " and " + max;
                            }
                        }
                    }
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                // Invalid validation format, skip custom validation
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * Validation result class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final String message;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
            this.message = null;
        }

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.errors = new ArrayList<>();
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getMessage() {
            return message;
        }
    }
}

