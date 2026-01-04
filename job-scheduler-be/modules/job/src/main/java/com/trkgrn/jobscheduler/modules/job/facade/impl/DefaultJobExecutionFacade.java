package com.trkgrn.jobscheduler.modules.job.facade.impl;

import com.trkgrn.jobscheduler.modules.job.dto.ExecutionStatsDto;
import com.trkgrn.jobscheduler.modules.job.dto.PaginatedResponse;
import com.trkgrn.jobscheduler.modules.job.facade.JobExecutionFacade;
import com.trkgrn.jobscheduler.modules.job.mapper.JobExecutionMapper;
import com.trkgrn.jobscheduler.modules.job.model.JobExecutionModel;
import com.trkgrn.jobscheduler.modules.job.service.JobExecutionService;
import com.trkgrn.jobscheduler.platform.common.dto.JobExecutionDto;
import com.trkgrn.jobscheduler.platform.common.model.exception.NotFoundException;
import com.trkgrn.jobscheduler.platform.common.model.exception.NotValidException;
import com.trkgrn.jobscheduler.platform.common.model.result.DataResult;
import com.trkgrn.jobscheduler.platform.common.model.result.SuccessDataResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DefaultJobExecutionFacade implements JobExecutionFacade {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultJobExecutionFacade.class);

    private final JobExecutionService jobExecutionService;
    private final JobExecutionMapper jobExecutionMapper;

    public DefaultJobExecutionFacade(JobExecutionService jobExecutionService, JobExecutionMapper jobExecutionMapper) {
        this.jobExecutionService = jobExecutionService;
        this.jobExecutionMapper = jobExecutionMapper;
    }

    @Override
    public DataResult<JobExecutionDto> findById(Long id) {
        JobExecutionModel execution = jobExecutionService.findById(id)
                .orElseThrow(() -> new NotFoundException("Job execution not found with id: " + id));
        JobExecutionDto executionDto = jobExecutionMapper.toDto(execution);
        return new SuccessDataResult<>(executionDto, "Job execution fetched successfully");
    }

    @Override
    public DataResult<List<JobExecutionDto>> findAll() {
        List<JobExecutionModel> executions = jobExecutionService.findAll();
        List<JobExecutionDto> executionDtos = executions.stream()
                .map(jobExecutionMapper::toDto)
                .collect(Collectors.toList());
        return new SuccessDataResult<>(executionDtos, "Job executions fetched successfully");
    }

    @Override
    public DataResult<List<JobExecutionDto>> findByCronJobId(Long cronJobId) {
        List<JobExecutionModel> executions = jobExecutionService.findByJobDefinitionId(cronJobId);
        List<JobExecutionDto> executionDtos = executions.stream()
                .map(jobExecutionMapper::toDto)
                .collect(Collectors.toList());
        return new SuccessDataResult<>(executionDtos, "Job executions fetched successfully");
    }

    @Override
    public DataResult<List<JobExecutionDto>> findByStatus(String status) {
        JobExecutionModel.Status executionStatus;
        try {
            executionStatus = JobExecutionModel.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NotValidException("Invalid status: " + status);
        }
        
        List<JobExecutionModel> executions = jobExecutionService.findByStatus(executionStatus);
        List<JobExecutionDto> executionDtos = executions.stream()
                .map(jobExecutionMapper::toDto)
                .collect(Collectors.toList());
        return new SuccessDataResult<>(executionDtos, "Job executions fetched successfully");
    }

    @Override
    public DataResult<List<JobExecutionDto>> findActive() {
        List<JobExecutionModel> executions = jobExecutionService.findActiveExecutions();
        List<JobExecutionDto> executionDtos = executions.stream()
                .map(jobExecutionMapper::toDto)
                .collect(Collectors.toList());
        return new SuccessDataResult<>(executionDtos, "Active job executions fetched successfully");
    }

    @Override
    public DataResult<ExecutionStatsDto> getStats(Long cronJobId) {
        long successCount;
        long failedCount;
        long totalCount;

        if (cronJobId != null) {
            successCount = jobExecutionService.countByJobDefinitionIdAndStatus(cronJobId, JobExecutionModel.Status.SUCCESS);
            failedCount = jobExecutionService.countByJobDefinitionIdAndStatus(cronJobId, JobExecutionModel.Status.FAILED);
            totalCount = jobExecutionService.findByJobDefinitionId(cronJobId).size();
        } else {
            successCount = jobExecutionService.countByJobDefinitionIdAndStatus(null, JobExecutionModel.Status.SUCCESS);
            failedCount = jobExecutionService.countByJobDefinitionIdAndStatus(null, JobExecutionModel.Status.FAILED);
            totalCount = jobExecutionService.findAll().size();
        }

        ExecutionStatsDto stats = new ExecutionStatsDto(successCount, failedCount, totalCount);
        return new SuccessDataResult<>(stats, "Execution stats fetched successfully");
    }

    @Override
    public DataResult<JobExecutionDto> cancel(Long id) {
        JobExecutionModel execution = jobExecutionService.findById(id)
                .orElseThrow(() -> new NotFoundException("Job execution not found with id: " + id));
        
        JobExecutionModel cancelledExecution = jobExecutionService.cancel(id);
        JobExecutionDto executionDto = jobExecutionMapper.toDto(cancelledExecution);
        return new SuccessDataResult<>(executionDto, "Job execution cancelled successfully");
    }

    @Override
    public DataResult<List<JobExecutionModel.LogEntry>> getLogs(Long id) {
        JobExecutionModel execution = jobExecutionService.findById(id)
                .orElseThrow(() -> new NotFoundException("Job execution not found with id: " + id));
        
        List<JobExecutionModel.LogEntry> logs = execution.getLogs();
        return new SuccessDataResult<>(logs != null ? logs : List.of(), "Logs fetched successfully");
    }

    @Override
    public DataResult<PaginatedResponse<JobExecutionDto>> findAllPaginated(int page, int size) {
        PaginatedResponse<JobExecutionModel> paginatedExecutions = jobExecutionService.findAllPaginated(page, size);
        List<JobExecutionDto> executionDtos = paginatedExecutions.getContent().stream()
                .map(jobExecutionMapper::toDto)
                .collect(Collectors.toList());
        
        PaginatedResponse<JobExecutionDto> paginatedResponse = new PaginatedResponse<>(
            executionDtos,
            paginatedExecutions.getPage(),
            paginatedExecutions.getSize(),
            paginatedExecutions.getTotalElements()
        );
        
        return new SuccessDataResult<>(paginatedResponse, "Paginated job executions fetched successfully");
    }

    @Override
    public DataResult<PaginatedResponse<JobExecutionDto>> findByCronJobIdPaginated(Long cronJobId, int page, int size, String status) {
        PaginatedResponse<JobExecutionModel> paginatedExecutions;
        
        if (status != null && !status.isEmpty()) {
            JobExecutionModel.Status executionStatus;
            try {
                executionStatus = JobExecutionModel.Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new NotValidException("Invalid status: " + status);
            }
            paginatedExecutions = jobExecutionService.findByJobDefinitionIdAndStatusPaginated(cronJobId, executionStatus, page, size);
        } else {
            paginatedExecutions = jobExecutionService.findByJobDefinitionIdPaginated(cronJobId, page, size);
        }
        
        List<JobExecutionDto> executionDtos = paginatedExecutions.getContent().stream()
                .map(jobExecutionMapper::toDto)
                .collect(Collectors.toList());
        
        PaginatedResponse<JobExecutionDto> paginatedResponse = new PaginatedResponse<>(
            executionDtos,
            paginatedExecutions.getPage(),
            paginatedExecutions.getSize(),
            paginatedExecutions.getTotalElements()
        );
        
        return new SuccessDataResult<>(paginatedResponse, "Paginated job executions fetched successfully");
    }

    @Override
    public DataResult<PaginatedResponse<JobExecutionDto>> findByStatusPaginated(String status, int page, int size) {
        JobExecutionModel.Status executionStatus;
        try {
            executionStatus = JobExecutionModel.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NotValidException("Invalid status: " + status);
        }
        
        PaginatedResponse<JobExecutionModel> paginatedExecutions = jobExecutionService.findByStatusPaginated(executionStatus, page, size);
        List<JobExecutionDto> executionDtos = paginatedExecutions.getContent().stream()
                .map(jobExecutionMapper::toDto)
                .collect(Collectors.toList());
        
        PaginatedResponse<JobExecutionDto> paginatedResponse = new PaginatedResponse<>(
            executionDtos,
            paginatedExecutions.getPage(),
            paginatedExecutions.getSize(),
            paginatedExecutions.getTotalElements()
        );
        
        return new SuccessDataResult<>(paginatedResponse, "Paginated job executions fetched successfully");
    }
}

