package com.trkgrn.jobscheduler.modules.job.service;

import com.trkgrn.jobscheduler.platform.common.dto.ExecutionTrendDataDto;
import com.trkgrn.jobscheduler.platform.common.dto.MinimalCronJobDto;
import com.trkgrn.jobscheduler.platform.common.dto.StatusDistributionDto;
import com.trkgrn.jobscheduler.platform.common.dto.TopJobDto;

import java.util.List;

public interface StatsService {
    long getTotalJobs();
    long getActiveJobs();
    long getRunningJobs();
    long getFailedJobs();
    long getTotalExecutions();
    long getSuccessfulExecutions(Long cronJobId);
    long getFailedExecutions(Long cronJobId);
    double getAverageExecutionTime(Long cronJobId);
    long getRunningExecutions();
    long getActiveExecutions();
    
    List<StatusDistributionDto> getJobStatusDistribution();
    List<StatusDistributionDto> getTriggerStatusDistribution();
    List<ExecutionTrendDataDto> getExecutionTrendData(int days);
    List<TopJobDto> getTopJobsByExecutionCount(int limit);
    List<MinimalCronJobDto> getAllCronJobsMinimal();
}
