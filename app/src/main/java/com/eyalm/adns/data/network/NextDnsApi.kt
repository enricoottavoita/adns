package com.eyalm.adns.data.network

import com.eyalm.adns.data.nextdns.rewrites.CreateRewriteRequest
import com.eyalm.adns.data.nextdns.access.InviteAccessRequest
import com.eyalm.adns.data.nextdns.access.UpdateAccessRoleRequest
import com.eyalm.adns.data.nextdns.recreation.UpdateRecreationItemRequest
import com.eyalm.adns.data.nextdns.recreation.UpdateRecreationScheduleRequest
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface NextDnsApi {

    @GET("profiles")
    suspend fun getProfiles(): NextDnsProfilesResponse

    @POST("profiles")
    suspend fun createProfile(
        @Body request: NextDnsCreateProfileRequest
    ): Response<NextDnsProfile>

    @GET("profiles/{profileId}/analytics/status")
    suspend fun getAnalytics(
        @Path("profileId") profileId: String,
        @Query("from") period: String
    ): NextDnsAnalytics

    @GET("profiles/{profileId}/analytics/status;series")
    suspend fun getStatsGraph(
        @Path("profileId") profileId: String,
        @Query("from") period: String,
        @Query("alignment") alignment: String = "start",
        @Query("timezone") timezone: String
    ): NextDnsStatsGraphResponse

    @GET("profiles/{profileId}/analytics/domains") // ?status=default%2Callowed&from=-30d&limit=6
    suspend fun getDomains(
        @Path("profileId") profileId: String,
        @Query("status") status: String, // "default,allowed," or "blocked"
        @Query("from") period: String,
        @Query("limit") limit: Int
    ): NextDnsDomainsResponse


    // NEW GENERIC ENDPOINTS

    @GET("profiles/{profileId}/{page}")
    suspend fun getPageSettings(
        @Path("profileId") profileId: String,
        @Path("page") page: String
    ): Response<JsonObject>


    @PATCH("profiles/{profileId}/{page}")
    suspend fun patchPageSettings(
        @Path("profileId") profileId: String,
        @Path("page") page: String,
        @Body payload: Map<String, @JvmSuppressWildcards Any>
    ): Response<JsonObject>

    // get active list items for a feature
    @GET("profiles/{profileId}/{page}/{feat}")
    suspend fun getActiveListItems(
        @Path("profileId") profileId: String,
        @Path("page") page: String,
        @Path("feat") feat: String
    ): JsonObject

    // get the available catalog for server lists
    @GET("{page}/{feat}")
    suspend fun getAvailableCatalog(
        @Path("page") page: String,
        @Path("feat") feat: String
    ): JsonObject

    // add an item to a list
    @POST("profiles/{profileId}/{page}/{feat}")
    suspend fun addListItem(
        @Path("profileId") profileId: String,
        @Path("page") page: String,
        @Path("feat") feat: String,
        @Body payload: Map<String, String>
    ): Response<Unit>

    // remove an item from a list with hex id.
    @DELETE("profiles/{profileId}/{page}/{feat}/{hexId}")
    suspend fun removeListItem(
        @Path("profileId") profileId: String,
        @Path("page") page: String,
        @Path("feat") feat: String,
        @Path("hexId") hexId: String
    ): Response<Unit>


    // endpoint for denylist/allowlist
    @POST("profiles/{profileId}/{page}")
    suspend fun addCustomItem(
        @Path("profileId") profileId: String,
        @Path("page") page: String,
        @Body payload: Map<String, String>
    ): Response<Unit>

    @PATCH("profiles/{profileId}/{page}/{hexId}")
    suspend fun patchCustomItem(
        @Path("profileId") profileId: String,
        @Path("page") page: String,
        @Path("hexId") hexId: String,
        @Body payload: Map<String, Boolean>
    ): Response<Unit>

    @DELETE("profiles/{profileId}/{page}/{hexId}")
    suspend fun removeCustomItem(
        @Path("profileId") profileId: String,
        @Path("page") page: String,
        @Path("hexId") hexId: String
    ): Response<Unit>

    // stats

    @GET("profiles/{profileId}/analytics/{feature}")
    suspend fun getAnalyticsFeature(
        @Path("profileId") profileId: String,
        @Path("feature") feature: String,
        @QueryMap params: Map<String, String>
    ): JsonObject


    @GET("profiles/{profileId}/logs")
    suspend fun getLogs(
        @Path("profileId") profileId: String,
        @Query("cursor") cursor: String? = null,
        @Query("device") device: String? = null,
        @Query("status") status: String? = null, // blocked / allowed / default / error
        @Query("search") search: String? = null,
        @Query("raw") raw: Int? = null,          // 1 = raw
        @Query("limit") limit: Int? = 100
    ): NextDnsLogsResponse

    @GET("profiles/{profileId}/analytics/devices")
    suspend fun getDevices(
        @Path("profileId") profileId: String,
        @Query("from") from: String = "-3M",
        @Query("limit") limit: Int = 200
    ): NextDnsDevicesResponse

    // rewrites

    @GET("profiles/{profileId}/rewrites")
    suspend fun getRewrites(
        @Path("profileId") profileId: String,
    ): Response<JsonObject>

    @POST("profiles/{profileId}/rewrites")
    suspend fun createRewrite(
        @Path("profileId") profileId: String,
        @Body request: CreateRewriteRequest,
    ): Response<JsonObject>

    @DELETE("profiles/{profileId}/rewrites/{rewriteId}")
    suspend fun deleteRewrite(
        @Path("profileId") profileId: String,
        @Path("rewriteId") rewriteId: String,
    ): Response<JsonObject>

    // profile access

    @GET("profiles/{profileId}/access")
    suspend fun getAccess(
        @Path("profileId") profileId: String,
    ): Response<JsonObject>

    @POST("profiles/{profileId}/access")
    suspend fun inviteAccess(
        @Path("profileId") profileId: String,
        @Body request: InviteAccessRequest,
    ): Response<JsonObject>

    @PATCH("profiles/{profileId}/access/{email}")
    suspend fun updateAccessRole(
        @Path("profileId") profileId: String,
        @Path("email") email: String,
        @Body request: UpdateAccessRoleRequest,
    ): Response<JsonObject>

    @DELETE("profiles/{profileId}/access/{email}")
    suspend fun deleteAccess(
        @Path("profileId") profileId: String,
        @Path("email") email: String,
    ): Response<JsonObject>

    // parental recreation

    @GET("profiles/{profileId}/parentalControl")
    suspend fun getParentalControl(
        @Path("profileId") profileId: String,
    ): Response<JsonObject>

    @PATCH("profiles/{profileId}/parentalControl")
    suspend fun updateRecreationSchedule(
        @Path("profileId") profileId: String,
        @Body request: UpdateRecreationScheduleRequest,
    ): Response<JsonObject>

    @PATCH("profiles/{profileId}/parentalControl/{collection}/{hexId}")
    suspend fun updateRecreationItem(
        @Path("profileId") profileId: String,
        @Path("collection") collection: String,
        @Path("hexId") hexId: String,
        @Body request: UpdateRecreationItemRequest,
    ): Response<JsonObject>

}
