package com.eyalm.adns.data.network

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NextDnsApi {

    @POST("accounts/@login")
    suspend fun login(
        @Body request: NextDnsLoginRequest
    ): Response<okhttp3.ResponseBody>

    @POST("account/apiKeys")
    suspend fun createApiKey(
        @Header("Cookie") cookie: String
    ): Response<NextDnsCreateApiKeyResponse>

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
    ): JsonObject


    @PATCH("profiles/{profileId}/{page}")
    suspend fun patchPageSettings(
        @Path("profileId") profileId: String,
        @Path("page") page: String,
        @Body payload: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

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

}