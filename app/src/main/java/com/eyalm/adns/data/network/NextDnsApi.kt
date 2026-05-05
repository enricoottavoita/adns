package com.eyalm.adns.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface NextDnsApi {

    @POST("accounts/@login")
    suspend fun login(
        @Body request: NextDnsLoginRequest
    ): Response<Unit>

    @GET("accounts/@me?withProfiles=true")
    suspend fun getProfiles(
        @Header("Cookie") cookie: String
    ): NextDnsProfileResponse

}