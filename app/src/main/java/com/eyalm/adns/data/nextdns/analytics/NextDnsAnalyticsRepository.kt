package com.eyalm.adns.data.nextdns.analytics

import com.eyalm.adns.data.network.ApiClient
import com.eyalm.adns.data.network.NextDnsApi
import com.eyalm.adns.data.network.NextDnsDeviceItem
import com.eyalm.adns.data.network.NextDnsStatsGraphResponse
import com.eyalm.adns.data.nextdns.api.toBodyApiResult
import com.eyalm.adns.data.nextdns.api.toJsonApiResult
import com.eyalm.adns.domain.nextdns.ApiResult
import com.google.gson.JsonArray
import java.io.IOException
import java.util.TimeZone
import kotlinx.coroutines.CancellationException

class NextDnsAnalyticsRepository(
    private val api: NextDnsApi = ApiClient.nextDnsApi,
) {
    suspend fun getGraph(
        profileId: String,
        scope: AnalyticsScope,
    ): ApiResult<NextDnsStatsGraphResponse> = apiCall {
        api.getStatsGraph(
            profileId = profileId,
            period = scope.period.wireValue,
            alignment = "start",
            timezone = TimeZone.getDefault().id,
            device = scope.deviceId,
        ).toBodyApiResult()
    }

    suspend fun getCardData(
        profileId: String,
        feature: String,
        baseParams: Map<String, String>,
        scope: AnalyticsScope,
    ): ApiResult<JsonArray> = apiCall {
        val params = buildMap {
            putAll(baseParams)
            put("from", scope.period.wireValue)
            scope.deviceId?.let { put("device", it) }
        }
        when (
            val result = api
                .getAnalyticsFeature(profileId, feature, params)
                .toJsonApiResult()
        ) {
            is ApiResult.Success -> {
                val data = result.value.getAsJsonArray("data")
                    ?: return@apiCall ApiResult.SerializationFailure(
                        IllegalStateException("Missing analytics data")
                    )
                ApiResult.Success(data, result.status)
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    suspend fun getDevices(profileId: String): ApiResult<List<NextDnsDeviceItem>> = apiCall {
        when (
            val result = api
                .getDevices(profileId)
                .toBodyApiResult()
        ) {
            is ApiResult.Success -> ApiResult.Success(result.value.data, result.status)
            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    private suspend inline fun <T> apiCall(
        block: () -> ApiResult<T>,
    ): ApiResult<T> = try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: IOException) {
        ApiResult.NetworkFailure(error)
    } catch (error: Exception) {
        ApiResult.SerializationFailure(error)
    }
}
