package com.trkgrn.jobscheduler.platform.common.model.result

sealed class Result @JvmOverloads constructor(
    var success: Boolean,
    var message: String? = null
) {
    companion object {
        @JvmStatic
        fun success(message: String? = null): Result = SuccessResult(message)
        
        @JvmStatic
        fun error(message: String?): Result = ErrorResult(null, message)
    }
}

class ErrorResult @JvmOverloads constructor(
    var errorDetail: Long? = null,
    message: String? = null
) : Result(success = false, message)

class SuccessResult @JvmOverloads constructor(
    message: String? = null
) : Result(success = true, message)

open class DataResult<T> @JvmOverloads constructor(
    var data: T?,
    success: Boolean,
    message: String? = null
) : Result(success, message) {
    companion object {
        @JvmStatic
        fun <T> success(data: T, message: String? = null): DataResult<T> = SuccessDataResult(data, message)
        
        @JvmStatic
        fun <T> error(data: T? = null, message: String?): DataResult<T> = ErrorDataResult(data, message)
    }
}

class ErrorDataResult<T> @JvmOverloads constructor(
    data: T? = null,
    message: String? = null
) : DataResult<T>(data, success = false, message)

class SuccessDataResult<T> @JvmOverloads constructor(
    data: T,
    message: String? = null
) : DataResult<T>(data, success = true, message)

