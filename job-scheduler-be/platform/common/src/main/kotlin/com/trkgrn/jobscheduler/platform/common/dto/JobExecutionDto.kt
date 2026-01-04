package com.trkgrn.jobscheduler.platform.common.dto

import java.io.Serializable
import java.time.OffsetDateTime

data class JobExecutionDto(
    var id: Long? = null,
    var jobDefinitionId: Long? = null,
    var status: String? = null,
    var startTime: OffsetDateTime? = null,
    var endTime: OffsetDateTime? = null,
    var duration: Long? = null,
    var result: String? = null,
    var errorMessage: String? = null,
    var retryCount: Int = 0,
    var correlationId: String? = null,
    var parameters: Map<String, Any?>? = null,
    var logs: List<LogEntryDto>? = null,
    var logLevel: String? = null,
    var createdAt: OffsetDateTime? = null,
    var updatedAt: OffsetDateTime? = null
) : Serializable

data class LogEntryDto(
    var timestamp: String? = null,
    var level: String? = null,
    var message: String? = null
) : Serializable

