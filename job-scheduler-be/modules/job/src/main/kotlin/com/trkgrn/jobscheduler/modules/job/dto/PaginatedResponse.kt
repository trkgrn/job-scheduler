package com.trkgrn.jobscheduler.modules.job.dto

data class PaginatedResponse<T>(
    var content: List<T> = emptyList(),
    var page: Int = 0,
    var size: Int = 0,
    var totalElements: Long = 0
) {
    val totalPages: Int
        get() = if (size > 0) kotlin.math.ceil(totalElements.toDouble() / size).toInt() else 0
    
    val first: Boolean
        get() = page == 0
    
    val last: Boolean
        get() = page >= totalPages - 1
    
    val hasNext: Boolean
        get() = !last
    
    val hasPrevious: Boolean
        get() = !first
}

