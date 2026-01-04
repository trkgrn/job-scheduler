package com.trkgrn.jobscheduler.modules.job.facade.impl;

import com.trkgrn.jobscheduler.modules.job.facade.StatsFacade;
import com.trkgrn.jobscheduler.modules.job.service.StatsService;
import com.trkgrn.jobscheduler.platform.common.dto.*;
import com.trkgrn.jobscheduler.platform.common.model.result.DataResult;
import com.trkgrn.jobscheduler.platform.common.model.result.SuccessDataResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultStatsFacade implements StatsFacade {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultStatsFacade.class);

    private final StatsService statsService;

    public DefaultStatsFacade(StatsService statsService) {
        this.statsService = statsService;
    }

    @Override
    public DataResult<JobStatsDto> getJobStats() {
        long totalJobs = statsService.getTotalJobs();
        long activeJobs = statsService.getActiveJobs();
        long runningJobs = statsService.getRunningJobs();
        long failedJobs = statsService.getFailedJobs();
        long totalExecutions = statsService.getTotalExecutions();
        long successfulExecutions = statsService.getSuccessfulExecutions(null);
        long failedExecutions = statsService.getFailedExecutions(null);
        double averageExecutionTime = statsService.getAverageExecutionTime(null);

        JobStatsDto stats = new JobStatsDto(
                totalJobs,
                activeJobs,
                runningJobs,
                failedJobs,
                totalExecutions,
                successfulExecutions,
                failedExecutions,
                averageExecutionTime
        );
        return new SuccessDataResult<>(stats, "Job stats fetched successfully");
    }

    @Override
    public DataResult<JobStatsDto> getExecutionStats(Long cronJobId) {
        long totalExecutions = statsService.getTotalExecutions();
        long successfulExecutions = statsService.getSuccessfulExecutions(cronJobId);
        long failedExecutions = statsService.getFailedExecutions(cronJobId);
        double averageExecutionTime = statsService.getAverageExecutionTime(cronJobId);

        JobStatsDto stats = new JobStatsDto(
                0L, // totalJobs - not relevant for execution stats
                0L, // activeJobs - not relevant for execution stats
                0L, // runningJobs - not relevant for execution stats
                0L, // failedJobs - not relevant for execution stats
                totalExecutions,
                successfulExecutions,
                failedExecutions,
                averageExecutionTime
        );
        return new SuccessDataResult<>(stats, "Execution stats fetched successfully");
    }

    @Override
    public DataResult<StatsOverviewDto> getOverviewStats(int days, int topJobsLimit) {
        JobStatsDto jobStats = getJobStats().getData();
        List<StatusDistributionDto> jobStatusDistribution = getJobStatusDistribution().getData();
        List<StatusDistributionDto> triggerStatusDistribution = getTriggerStatusDistribution().getData();
        List<ExecutionTrendDataDto> executionTrend = getExecutionTrend(days).getData();
        List<TopJobDto> topJobs = getTopJobsByExecution(topJobsLimit).getData();
        List<MinimalCronJobDto> cronJobs = statsService.getAllCronJobsMinimal();

        StatsOverviewDto overview = new StatsOverviewDto(
                jobStats,
                jobStatusDistribution,
                triggerStatusDistribution,
                executionTrend,
                topJobs,
                cronJobs
        );
        return new SuccessDataResult<>(overview, "Overview stats fetched successfully");
    }

    @Override
    public DataResult<List<StatusDistributionDto>> getJobStatusDistribution() {
        List<StatusDistributionDto> distribution = statsService.getJobStatusDistribution();
        return new SuccessDataResult<>(distribution, "Job status distribution fetched successfully");
    }

    @Override
    public DataResult<List<StatusDistributionDto>> getTriggerStatusDistribution() {
        List<StatusDistributionDto> distribution = statsService.getTriggerStatusDistribution();
        return new SuccessDataResult<>(distribution, "Trigger status distribution fetched successfully");
    }

    @Override
    public DataResult<List<ExecutionTrendDataDto>> getExecutionTrend(int days) {
        List<ExecutionTrendDataDto> trend = statsService.getExecutionTrendData(days);
        return new SuccessDataResult<>(trend, "Execution trend fetched successfully");
    }

    @Override
    public DataResult<List<TopJobDto>> getTopJobsByExecution(int limit) {
        List<TopJobDto> topJobs = statsService.getTopJobsByExecutionCount(limit);
        return new SuccessDataResult<>(topJobs, "Top jobs by execution fetched successfully");
    }
}

