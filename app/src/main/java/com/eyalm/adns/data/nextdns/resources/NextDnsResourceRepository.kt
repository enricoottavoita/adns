package com.eyalm.adns.data.nextdns.resources

import com.eyalm.adns.data.network.ApiClient
import com.eyalm.adns.data.nextdns.api.toJsonApiResult
import com.eyalm.adns.data.nextdns.model.ListIcon
import com.eyalm.adns.data.nextdns.model.nextDnsFaviconUrl
import com.eyalm.adns.domain.nextdns.ApiResult
import java.io.IOException
import kotlinx.coroutines.CancellationException

data class CustomResourceList(
    val activeIds: Set<String>,
    val items: List<NextDnsResourceItem>,
)

class NextDnsResourceRepository {
    suspend fun getActiveIds(
        profileId: String,
        page: String,
        feature: String,
    ): ApiResult<Set<String>> = apiCall {
        when (
            val result = ApiClient.nextDnsApi
                .getActiveListItems(profileId, page, feature)
                .toJsonApiResult()
        ) {
            is ApiResult.Success -> {
                val data = result.value.getAsJsonArray("data")
                    ?: return@apiCall ApiResult.SerializationFailure(
                        IllegalStateException("Missing active resource data")
                    )
                ApiResult.Success(
                    data.mapNotNull { element ->
                        element.takeIf { it.isJsonObject }
                            ?.asJsonObject
                            ?.get("id")
                            ?.takeIf { it.isJsonPrimitive }
                            ?.asString
                    }.toSet(),
                    result.status,
                )
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    suspend fun getServerCatalog(
        page: String,
        feature: String,
    ): ApiResult<List<NextDnsResourceItem>> = apiCall {
        when (
            val result = ApiClient.nextDnsApi
                .getAvailableCatalog(page, feature)
                .toJsonApiResult()
        ) {
            is ApiResult.Success -> {
                val data = result.value.getAsJsonArray("data")
                    ?: return@apiCall ApiResult.SerializationFailure(
                        IllegalStateException("Missing resource catalog data")
                    )
                ApiResult.Success(
                    mapServerResourceItems(feature, data),
                    result.status,
                )
            }

            is ApiResult.ServerFailure -> result
            is ApiResult.NetworkFailure -> result
            is ApiResult.SerializationFailure -> result
        }
    }

    suspend fun getCustomList(
        profileId: String,
        page: String,
    ): ApiResult<CustomResourceList> = apiCall {
        when (
            val result = ApiClient.nextDnsApi
                .getPageSettings(profileId, page)
                .toJsonApiResult()
        ) {
            is ApiResult.Success -> {
                val data = result.value.getAsJsonArray("data")
                    ?: return@apiCall ApiResult.SerializationFailure(
                        IllegalStateException("Missing custom list data")
                    )
                val activeIds = mutableSetOf<String>()
                val items = data.mapNotNull { element ->
                    val item = element.takeIf { it.isJsonObject }?.asJsonObject
                        ?: return@mapNotNull null
                    val id = item.get("id")
                        ?.takeIf { it.isJsonPrimitive }
                        ?.asString
                        ?: return@mapNotNull null
                    val active = item.get("active")
                        ?.takeIf { it.isJsonPrimitive }
                        ?.asBoolean
                        ?: true
                    if (active) activeIds += id
                    NextDnsResourceItem(
                        id = id,
                        name = "*.$id",
                        icon = nextDnsFaviconUrl(id)
                            ?.let(ListIcon::Url)
                            ?: ListIcon.None,
                    )
                }
                ApiResult.Success(
                    CustomResourceList(activeIds, items),
                    result.status,
                )
            }

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
