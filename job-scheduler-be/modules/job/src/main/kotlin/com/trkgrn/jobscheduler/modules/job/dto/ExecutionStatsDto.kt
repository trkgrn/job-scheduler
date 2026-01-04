package com.trkgrn.jobscheduler.modules.job.dto

import java.io.Serializable

data class ExecutionStatsDto(
    val successCount: Long,
    val failedCount: Long,
    val totalCount: Long
) : Serializable {
    
    val successRate: Double
        get() = if (totalCount > 0) (successCount.toDouble() / totalCount) * 100 else 0.0
    
    val failureRate: Double
        get() = if (totalCount > 0) (failedCount.toDouble() / totalCount) * 100 else 0.0
}

