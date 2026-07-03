package com.eyalm.adns.data.nextdns.logs

import com.eyalm.adns.domain.nextdns.ApiResult

sealed interface LogExportResult {
    data object Success : LogExportResult
    data class ApiFailure(val result: ApiResult<Nothing>) : LogExportResult
    data class DestinationFailure(val cause: Throwable) : LogExportResult
}
