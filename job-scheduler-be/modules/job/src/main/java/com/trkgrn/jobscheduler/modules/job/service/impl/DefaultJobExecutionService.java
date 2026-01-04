package com.trkgrn.jobscheduler.modules.job.service.impl;

import com.trkgrn.jobscheduler.modules.job.api.JobExecutionContext;
import com.trkgrn.jobscheduler.modules.job.dto.PaginatedResponse;
import com.trkgrn.jobscheduler.modules.job.model.JobExecutionModel;
import com.trkgrn.jobscheduler.modules.job.repository.JobExecutionRepository;
import com.trkgrn.jobscheduler.modules.job.service.JobExecutionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DefaultJobExecutionService implements JobExecutionService {

    private final JobExecutionRepository jobExecutionRepository;
    private final JobExecutionContext jobExecutionContext;

    public DefaultJobExecutionService(JobExecutionRepository jobExecutionRepository, JobExecutionContext jobExecutionContext) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.jobExecutionContext = jobExecutionContext;
    }

    @Override
    public Optional<JobExecutionModel> findById(Long id) {
        return jobExecutionRepository.findById(id);
    }

    @Override
    public List<JobExecutionModel> findByJobDefinitionId(Long jobDefinitionId) {
        return jobExecutionRepository.findByJobDefinitionId(jobDefinitionId);
    }

    @Override
    @Transactional
    public JobExecutionModel cancel(Long id) {
        JobExecutionModel execution = jobExecutionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job execution not found with id: " + id));

        // Only cancel if job is currently running
        if (execution.getStatus() != JobExecutionModel.Status.RUNNING) {
            throw new RuntimeException("Cannot cancel job execution. Status is: " + execution.getStatus());
        }

        execution.setStatus(JobExecutionModel.Status.CANCELLED);
        execution.setEndedAt(java.time.OffsetDateTime.now());
        execution = jobExecutionRepository.save(execution);
        
        // Update in-memory cancellation flag for fast job response
        jobExecutionContext.requestCancellation(id);
        
        return execution;
    }


    @Override
    @Transactional
    public JobExecutionModel save(JobExecutionModel execution) {
        return jobExecutionRepository.save(execution);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jobExecutionRepository.deleteById(id);
    }

    @Override
    public List<JobExecutionModel> findAll() {
        return jobExecutionRepository.findAll();
    }

    @Override
    public List<JobExecutionModel> findByStatus(JobExecutionModel.Status status) {
        return jobExecutionRepository.findByStatus(status);
    }

    @Override
    public List<JobExecutionModel> findActiveExecutions() {
        return jobExecutionRepository.findByIsActiveTrue();
    }

    @Override
    public Long countByJobDefinitionIdAndStatus(Long jobId, JobExecutionModel.Status status) {
        if (jobId != null) {
            return jobExecutionRepository.countByJobDefinitionIdAndStatus(jobId, status);
        } else {
            // Count all executions with this status
            return jobExecutionRepository.findAll().stream()
                    .filter(execution -> execution.getStatus() == status)
                    .count();
        }
    }

    // Paginated methods
    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<JobExecutionModel> findAllPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"));
        Page<JobExecutionModel> pageResult = jobExecutionRepository.findAllByOrderByStartedAtDesc(pageable);
        
        return new PaginatedResponse<>(
            pageResult.getContent(),
            pageResult.getNumber(),
            pageResult.getSize(),
            pageResult.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<JobExecutionModel> findByJobDefinitionIdPaginated(Long jobDefinitionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"));
        Page<JobExecutionModel> pageResult = jobExecutionRepository.findByJobDefinitionIdWithPagination(jobDefinitionId, pageable);
        
        return new PaginatedResponse<>(
            pageResult.getContent(),
            pageResult.getNumber(),
            pageResult.getSize(),
            pageResult.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<JobExecutionModel> findByStatusPaginated(JobExecutionModel.Status status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"));
        Page<JobExecutionModel> pageResult = jobExecutionRepository.findByStatusOrderByStartedAtDesc(status, pageable);
        
        return new PaginatedResponse<>(
            pageResult.getContent(),
            pageResult.getNumber(),
            pageResult.getSize(),
            pageResult.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<JobExecutionModel> findByJobDefinitionIdAndStatusPaginated(Long jobDefinitionId, JobExecutionModel.Status status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"));
        Page<JobExecutionModel> pageResult = jobExecutionRepository.findByJobDefinitionIdAndStatusWithPagination(jobDefinitionId, status, pageable);
        
        return new PaginatedResponse<>(
            pageResult.getContent(),
            pageResult.getNumber(),
            pageResult.getSize(),
            pageResult.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRunningExecution(Long jobDefinitionId) {
        List<JobExecutionModel> runningExecutions = jobExecutionRepository.findRunningByJobDefinitionId(jobDefinitionId);
        return !runningExecutions.isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobExecutionModel> findRunningByJobDefinitionId(Long jobDefinitionId) {
        return jobExecutionRepository.findRunningByJobDefinitionId(jobDefinitionId);
    }
}

