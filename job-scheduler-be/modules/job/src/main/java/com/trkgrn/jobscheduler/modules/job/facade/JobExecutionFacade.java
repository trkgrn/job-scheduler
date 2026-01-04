package com.trkgrn.jobscheduler.modules.job.facade;

import com.trkgrn.jobscheduler.modules.job.dto.ExecutionStatsDto;
import com.trkgrn.jobscheduler.modules.job.dto.PaginatedResponse;
import com.trkgrn.jobscheduler.modules.job.model.JobExecutionModel;
import com.trkgrn.jobscheduler.platform.common.dto.JobExecutionDto;
import com.trkgrn.jobscheduler.platform.common.model.result.DataResult;

import java.util.List;

public interface JobExecutionFacade {
    DataResult<JobExecutionDto> findById(Long id);
    DataResult<List<JobExecutionDto>> findAll();
    DataResult<List<JobExecutionDto>> findByCronJobId(Long cronJobId);
    DataResult<List<JobExecutionDto>> findByStatus(String status);
    DataResult<List<JobExecutionDto>> findActive();
    DataResult<ExecutionStatsDto> getStats(Long cronJobId);
    DataResult<JobExecutionDto> cancel(Long id);
    DataResult<List<JobExecutionModel.LogEntry>> getLogs(Long id);
    DataResult<PaginatedResponse<JobExecutionDto>> findAllPaginated(int page, int size);
    DataResult<PaginatedResponse<JobExecutionDto>> findByCronJobIdPaginated(Long cronJobId, int page, int size, String status);
    DataResult<PaginatedResponse<JobExecutionDto>> findByStatusPaginated(String status, int page, int size);
}

