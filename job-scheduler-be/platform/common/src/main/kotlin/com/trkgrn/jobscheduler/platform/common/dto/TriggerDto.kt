package com.trkgrn.jobscheduler.platform.common.dto

import java.io.Serializable
import java.time.OffsetDateTime

data class TriggerDto(
    var id: Long? = null,
    var name: String? = null,
    var description: String? = null,
    var cronExpression: String? = null,
    var enabled: Boolean = true,
    var startTime: OffsetDateTime? = null,
    var endTime: OffsetDateTime? = null,
    var misfireInstruction: String? = "DO_NOTHING",
    var priority: Int = 5,
    var cronJobId: Long? = null,
    var cronJob: CronJobDto? = null,
    var nextFireTime: OffsetDateTime? = null,
    var createdAt: OffsetDateTime? = null,
    var updatedAt: OffsetDateTime? = null
) : Serializable

