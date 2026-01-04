package com.trkgrn.jobscheduler.modules.job.facade;

import com.trkgrn.jobscheduler.platform.common.dto.*;
import com.trkgrn.jobscheduler.platform.common.model.result.DataResult;

import java.util.List;

public interface StatsFacade {
    DataResult<JobStatsDto> getJobStats();
    DataResult<JobStatsDto> getExecutionStats(Long cronJobId);
    DataResult<StatsOverviewDto> getOverviewStats(int days, int topJobsLimit);
    DataResult<List<StatusDistributionDto>> getJobStatusDistribution();
    DataResult<List<StatusDistributionDto>> getTriggerStatusDistribution();
    DataResult<List<ExecutionTrendDataDto>> getExecutionTrend(int days);
    DataResult<List<TopJobDto>> getTopJobsByExecution(int limit);
}

