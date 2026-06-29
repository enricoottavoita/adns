package com.eyalm.adns.data.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST


// NextDNS authentication calls!
interface NextDnsAuthApi {

    @POST("accounts/@login")
    suspend fun login(
        @Body request: NextDnsLoginRequest,
    ): Response<ResponseBody>

    @POST("account/apiKeys")
    suspend fun exchangeCookieForApiKey(
        @Header("Cookie") cookie: String,
    ): Response<NextDnsCreateApiKeyResponse>

    @GET("profiles")
    suspend fun verifyApiKey(
        @Header("X-Api-Key") apiKey: String,
    ): Response<NextDnsProfilesResponse>
}
