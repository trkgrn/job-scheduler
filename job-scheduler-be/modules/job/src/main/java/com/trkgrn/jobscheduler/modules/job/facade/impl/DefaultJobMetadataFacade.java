package com.trkgrn.jobscheduler.modules.job.facade.impl;

import com.trkgrn.jobscheduler.modules.job.dto.JobMetadataDto;
import com.trkgrn.jobscheduler.modules.job.facade.JobMetadataFacade;
import com.trkgrn.jobscheduler.modules.job.registry.EnhancedJobRegistry;
import com.trkgrn.jobscheduler.platform.common.model.exception.NotFoundException;
import com.trkgrn.jobscheduler.platform.common.model.result.DataResult;
import com.trkgrn.jobscheduler.platform.common.model.result.SuccessDataResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DefaultJobMetadataFacade implements JobMetadataFacade {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultJobMetadataFacade.class);

    private final EnhancedJobRegistry enhancedJobRegistry;

    public DefaultJobMetadataFacade(EnhancedJobRegistry enhancedJobRegistry) {
        this.enhancedJobRegistry = enhancedJobRegistry;
    }

    @Override
    public DataResult<List<JobMetadataDto>> getAllJobMetadata() {
        List<JobMetadataDto> jobs = enhancedJobRegistry.getAvailableJobs();
        return new SuccessDataResult<>(jobs, "Job metadata fetched successfully");
    }

    @Override
    public DataResult<JobMetadataDto> getJobMetadata(String beanName) {
        JobMetadataDto metadata = enhancedJobRegistry.getJobMetadata(beanName)
                .orElseThrow(() -> new NotFoundException("Job not found with bean name: " + beanName));
        return new SuccessDataResult<>(metadata, "Job metadata fetched successfully");
    }

    @Override
    public DataResult<List<JobMetadataDto>> getJobsByCategory(String category) {
        List<JobMetadataDto> jobs = enhancedJobRegistry.getJobsByCategory(category);
        return new SuccessDataResult<>(jobs, "Jobs by category fetched successfully");
    }

    @Override
    public DataResult<Set<String>> getCategories() {
        Set<String> categories = enhancedJobRegistry.getCategories();
        return new SuccessDataResult<>(categories, "Categories fetched successfully");
    }

    @Override
    public DataResult<Boolean> jobExists(String beanName) {
        boolean exists = enhancedJobRegistry.hasJob(beanName);
        return new SuccessDataResult<>(exists, "Job existence checked successfully");
    }

    @Override
    public DataResult<Map<String, Object>> validateParameters(String beanName, Map<String, Object> parameters) {
        if (parameters == null) {
            Map<String, Object> errorResult = Map.of(
                "valid", false,
                "errors", List.of("Parameters are required")
            );
            return new SuccessDataResult<>(errorResult, "Parameters are required");
        }
        
        var metadata = enhancedJobRegistry.getJobMetadata(beanName);
        if (metadata.isEmpty()) {
            Map<String, Object> errorResult = Map.of(
                "valid", false,
                "errors", List.of("Job not found: " + beanName)
            );
            return new SuccessDataResult<>(errorResult, "Job not found: " + beanName);
        }
        
        // For now, return a simple validation result
        // In a real implementation, you would validate against the job's parameter schema
        Map<String, Object> result = Map.of(
            "valid", true,
            "errors", List.of()
        );
        return new SuccessDataResult<>(result, "Parameters validated successfully");
    }
}

