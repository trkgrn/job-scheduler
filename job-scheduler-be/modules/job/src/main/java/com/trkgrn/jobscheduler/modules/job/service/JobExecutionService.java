package com.trkgrn.jobscheduler.modules.job.service;

import com.trkgrn.jobscheduler.modules.job.dto.PaginatedResponse;
import com.trkgrn.jobscheduler.modules.job.model.JobExecutionModel;

import java.util.List;
import java.util.Optional;

public interface JobExecutionService {
    Optional<JobExecutionModel> findById(Long id);
    List<JobExecutionModel> findByJobDefinitionId(Long jobDefinitionId);
    JobExecutionModel cancel(Long id);
    JobExecutionModel save(JobExecutionModel execution);
    void deleteById(Long id);
    List<JobExecutionModel> findAll();
    List<JobExecutionModel> findByStatus(JobExecutionModel.Status status);
    List<JobExecutionModel> findActiveExecutions();
    Long countByJobDefinitionIdAndStatus(Long jobId, JobExecutionModel.Status status);
    PaginatedResponse<JobExecutionModel> findAllPaginated(int page, int size);
    PaginatedResponse<JobExecutionModel> findByJobDefinitionIdPaginated(Long jobDefinitionId, int page, int size);
    PaginatedResponse<JobExecutionModel> findByStatusPaginated(JobExecutionModel.Status status, int page, int size);
    PaginatedResponse<JobExecutionModel> findByJobDefinitionIdAndStatusPaginated(Long jobDefinitionId, JobExecutionModel.Status status, int page, int size);
    boolean hasRunningExecution(Long jobDefinitionId);
    List<JobExecutionModel> findRunningByJobDefinitionId(Long jobDefinitionId);
}
