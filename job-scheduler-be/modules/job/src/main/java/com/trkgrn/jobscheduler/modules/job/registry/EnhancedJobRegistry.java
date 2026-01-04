package com.trkgrn.jobscheduler.modules.job.registry;

import com.trkgrn.jobscheduler.modules.job.annotation.JobComponent;
import com.trkgrn.jobscheduler.modules.job.annotation.JobParameter;
import com.trkgrn.jobscheduler.modules.job.api.Job;
import com.trkgrn.jobscheduler.modules.job.dto.JobMetadataDto;
import com.trkgrn.jobscheduler.modules.job.dto.JobParameterDto;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced job registry with metadata discovery
 * Scans for @JobComponent annotations and builds job metadata
 */
@Service
public class EnhancedJobRegistry {
    
    private static final Logger LOG = LoggerFactory.getLogger(EnhancedJobRegistry.class);
    
    private final ApplicationContext applicationContext;
    private final Map<String, JobMetadataDto> jobMetadataMap = new HashMap<>();
    private final Map<String, Job<?>> jobInstances = new HashMap<>();
    
    @Autowired
    public EnhancedJobRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    @PostConstruct
    public void initializeJobRegistry() {
        LOG.info("Initializing enhanced job registry...");
        
        // Get all job beans from application context
        Map<String, Job> jobBeans = applicationContext.getBeansOfType(Job.class);
        
        for (Map.Entry<String, Job> entry : jobBeans.entrySet()) {
            String beanName = entry.getKey();
            Job<?> job = entry.getValue();
            
            try {
                // Check if job has @JobComponent annotation
                JobComponent annotation = job.getClass().getAnnotation(JobComponent.class);
                
                if (annotation != null) {
                    // Build job metadata
                    JobMetadataDto metadata = buildJobMetadata(beanName, annotation, job);
                    jobMetadataMap.put(beanName, metadata);
                    jobInstances.put(beanName, job);
                    
                    LOG.info("Registered job: {} - {} (abortable: {})", beanName, metadata.getDisplayName(), metadata.getAbortable());
                } else {
                    // Fallback for jobs without annotation
                    JobMetadataDto metadata = buildFallbackMetadata(beanName, job);
                    jobMetadataMap.put(beanName, metadata);
                    jobInstances.put(beanName, job);
                    
                    LOG.info("Registered job (fallback): {} - {} (abortable: {})", beanName, metadata.getDisplayName(), metadata.getAbortable());
                }
            } catch (Exception e) {
                LOG.error("Failed to register job: {}", beanName, e);
            }
        }
        
        LOG.info("Job registry initialization completed. Registered {} jobs.", jobMetadataMap.size());
    }
    
    /**
     * Get all available job metadata
     */
    public List<JobMetadataDto> getAvailableJobs() {
        return new ArrayList<>(jobMetadataMap.values());
    }
    
    /**
     * Get job metadata by bean name
     */
    public Optional<JobMetadataDto> getJobMetadata(String beanName) {
        return Optional.ofNullable(jobMetadataMap.get(beanName));
    }
    
    /**
     * Get job instance by bean name
     */
    public Optional<Job<?>> getJobInstance(String beanName) {
        return Optional.ofNullable(jobInstances.get(beanName));
    }
    
    /**
     * Check if job exists
     */
    public boolean hasJob(String beanName) {
        return jobMetadataMap.containsKey(beanName);
    }
    
    /**
     * Get jobs by category
     */
    public List<JobMetadataDto> getJobsByCategory(String category) {
        return jobMetadataMap.values().stream()
                .filter(job -> category.equals(job.getCategory()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get all categories
     */
    public Set<String> getCategories() {
        return jobMetadataMap.values().stream()
                .map(JobMetadataDto::getCategory)
                .collect(Collectors.toSet());
    }
    
    private JobMetadataDto buildJobMetadata(String beanName, JobComponent annotation, Job<?> job) {
        List<JobParameterDto> parameters = Arrays.stream(annotation.parameters())
                .map(this::buildParameterDto)
                .collect(Collectors.toList());
        
        // Get abortable status from job instance
        boolean isAbortable = job.isAbortable();
        
        return new JobMetadataDto(
                beanName,
                annotation.displayName(),
                annotation.description(),
                annotation.category(),
                parameters,
                isAbortable
        );
    }
    
    private JobMetadataDto buildFallbackMetadata(String beanName, Job<?> job) {
        // Get abortable status from job instance
        boolean isAbortable = job.isAbortable();
        
        return new JobMetadataDto(
                beanName,
                job.getJobName(),
                job.getDescription(),
                "GENERAL",
                Collections.emptyList(),
                isAbortable
        );
    }
    
    private JobParameterDto buildParameterDto(JobParameter parameter) {
        return new JobParameterDto(
                parameter.name(),
                parameter.type().name(),
                parameter.displayName(),
                parameter.description(),
                parameter.required(),
                parameter.defaultValue(),
                parameter.validation(),
                parameter.options()
        );
    }
}

