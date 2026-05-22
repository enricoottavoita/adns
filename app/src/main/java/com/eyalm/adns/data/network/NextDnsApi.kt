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
    ): Response<Unit>

    @GET("accounts/@me?withProfiles=true")
    suspend fun getProfiles(
        @Header("Cookie") cookie: String
    ): NextDnsProfileResponse

    @POST("profiles")
    suspend fun createProfile(
        @Header("Cookie") cookie: String,
        @Body request: NextDnsCreateProfileRequest
    ): Response<NextDnsProfile>

    @GET("profiles/{profileId}/analytics/status")
    suspend fun getAnalytics(
        @Header("Cookie") cookie: String,
        @Path("profileId") profileId: String,
        @Query("from") period: String
    ): NextDnsAnalytics

    @GET("profiles/{profileId}/analytics/status;series")
    suspend fun getStatsGraph(
        @Header("Cookie") cookie: String,
        @Path("profileId") profileId: String,
        @Query("from") period: String,
        @Query("alignment") alignment: String = "start",
        @Query("timezone") timezone: String
    ): NextDnsStatsGraphResponse

    @GET("profiles/{profileId}/analytics/domains") // ?status=default%2Callowed&from=-30d&limit=6
    suspend fun getDomains(
        @Header("Cookie") cookie: String,
        @Path("profileId") profileId: String,
        @Query("status") status: String, // "default,allowed," or "blocked"
        @Query("from") period: String,
        @Query("limit") limit: Int
    ): NextDnsDomainsResponse


    // NEW GENERIC ENDPOINTS

    @GET("profiles/{profileId}/{page}")
    suspend fun getPageSettings(
        @Header("Cookie") cookie: String,
        @Path("profileId") profileId: String,
        @Path("page") page: String
    ): JsonObject


    @PATCH("profiles/{profileId}/{page}")
    suspend fun patchPageSettings(
        @Header("Cookie") cookie: String,
        @Path("profileId") profileId: String,
        @Path("page") page: String,
        @Body payload: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    // get active list items for a feature
    @GET("profiles/{profileId}/{page}/{feat}")
    suspend fun getActiveListItems(
        @Header("Cookie") cookie: String,
        @Path("profileId") profileId: String,
        @Path("page") page: String,
        @Path("feat") feat: String
    ): JsonObject

    // get the available catalog for server lists
    @GET("{page}/{feat}")
    suspend fun getAvailableCatalog(
        @Header("Cookie") cookie: String,
        @Path("page") page: String,
        @Path("feat") feat: String
    ): JsonObject

    // add an item to a list
    @POST("profiles/{profileId}/{page}/{feat}")
    suspend fun addListItem(
        @Header("Cookie") cookie: String,
        @Path("profileId") profileId: String,
        @Path("page") page: String,
        @Path("feat") feat: String,
        @Body payload: Map<String, String>
    ): Response<Unit>

    // remove an item from a list with hex id.
    @DELETE("profiles/{profileId}/{page}/{feat}/{hexId}")
    suspend fun removeListItem(
        @Header("Cookie") cookie: String,
        @Path("profileId") profileId: String,
        @Path("page") page: String,
        @Path("feat") feat: String,
        @Path("hexId") hexId: String
    ): Response<Unit>





    // OLD ENDPOINTS
    // TODO get rid of them
    @GET("privacy/blocklists")
    suspend fun getBlocklists(
        @Header("Cookie") cookie: String
    ): NextDnsBlocklistResponse

    @GET("profiles/{profileId}/privacy")
    suspend fun getPrivacy(
        @Header("Cookie") cookie: String,
        @Path("profileId") profileId: String
    ): NextDnsPrivacyResponse

    @POST("profiles/{profileId}/privacy/blocklists")
    suspend fun addBlocklist(
        @Header("Cookie") cookie: String,
        @Path("profileId") profileId: String,
        @Body body: NextDnsUpdateBlocklistsRequest
    ): Response<Unit>

    @DELETE("profiles/{profileId}/privacy/blocklists/{blocklistId}")
    suspend fun removeBlocklist(
        @Header("Cookie") cookie: String,
        @Path("profileId") profileId: String,
        @Path("blocklistId") blocklistId: String
    ): Response<Unit>








}