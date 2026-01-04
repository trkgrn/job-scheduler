package com.trkgrn.jobscheduler.platform.common.dto

import java.io.Serializable

data class JobStatsDto(
    val totalJobs: Long = 0,
    val activeJobs: Long = 0,
    val runningJobs: Long = 0,
    val failedJobs: Long = 0,
    val totalExecutions: Long = 0,
    val successfulExecutions: Long = 0,
    val failedExecutions: Long = 0,
    val averageExecutionTime: Double = 0.0
) : Serializable

