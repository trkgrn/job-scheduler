package com.trkgrn.jobscheduler.modules.job.dto

import java.io.Serializable

data class JobMetadataDto(
    var beanName: String? = null,
    var displayName: String? = null,
    var description: String? = null,
    var category: String? = null,
    var parameters: List<JobParameterDto>? = null,
    var abortable: Boolean = false
) : Serializable

