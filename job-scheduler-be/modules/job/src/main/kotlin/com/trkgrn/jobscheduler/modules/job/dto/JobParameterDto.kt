package com.trkgrn.jobscheduler.modules.job.dto

import java.io.Serializable

data class JobParameterDto(
    var name: String? = null,
    var type: String? = null,
    var displayName: String? = null,
    var description: String? = null,
    var required: Boolean = false,
    var defaultValue: String? = null,
    var validation: String? = null,
    var options: String? = null
) : Serializable

