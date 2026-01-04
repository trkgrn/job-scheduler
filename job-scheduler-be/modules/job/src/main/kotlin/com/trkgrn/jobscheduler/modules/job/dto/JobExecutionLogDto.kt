package com.trkgrn.jobscheduler.modules.job.dto

import java.io.Serializable
import java.time.OffsetDateTime

data class JobExecutionLogDto(
    var id: Long? = null,
    var executionId: Long? = null,
    var timestamp: OffsetDateTime? = null,
    var level: String? = null,
    var message: String? = null
) : Serializable

