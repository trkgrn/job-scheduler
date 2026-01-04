package com.trkgrn.jobscheduler.platform.common.dto

import java.io.Serializable
import java.time.OffsetDateTime

data class CronJobDto(
    var id: Long? = null,
    var code: String? = null,
    var name: String? = null,
    var description: String? = null,
    var jobBeanName: String? = null,
    var enabled: Boolean = true,
    var status: String? = null,
    var lastStartTime: OffsetDateTime? = null,
    var lastEndTime: OffsetDateTime? = null,
    var lastResult: String? = null,
    var retryCount: Int = 0,
    var maxRetries: Int = 3,
    var parameters: Map<String, Any?>? = null,
    var logLevel: String? = "INFO",
    var createdAt: OffsetDateTime? = null,
    var updatedAt: OffsetDateTime? = null
) : Serializable

