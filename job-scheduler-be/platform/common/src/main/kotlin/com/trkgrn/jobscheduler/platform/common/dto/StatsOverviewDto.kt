package com.trkgrn.jobscheduler.platform.common.dto

import java.io.Serializable

data class StatusDistributionDto(
    val status: String,
    val count: Long
) : Serializable

data class ExecutionTrendDataDto(
    val date: String,
    val successful: Long,
    val failed: Long,
    val total: Long
) : Serializable

data class TopJobDto(
    val jobId: Long,
    val jobName: String,
    val executionCount: Long
) : Serializable

data class StatsOverviewDto(
    val jobStats: JobStatsDto,
    val jobStatusDistribution: List<StatusDistributionDto>,
    val triggerStatusDistribution: List<StatusDistributionDto>,
    val executionTrend: List<ExecutionTrendDataDto>,
    val topJobsByExecution: List<TopJobDto>,
    val cronJobs: List<MinimalCronJobDto>
) : Serializable

data class MinimalCronJobDto(
    val id: Long,
    val name: String
) : Serializable

