package com.eyalm.adns.data.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface NextDnsLinkIpApi {
    @GET("{profileId}/{updateToken}")
    suspend fun linkCurrentIp(
        @Path("profileId") profileId: String,
        @Path("updateToken") updateToken: String,
    ): Response<ResponseBody>
}
