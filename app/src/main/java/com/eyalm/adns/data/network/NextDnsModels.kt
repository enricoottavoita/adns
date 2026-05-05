package com.eyalm.adns.data.network

import com.google.gson.annotations.SerializedName

data class NextDnsLoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class NextDnsProfileResponse(
    @SerializedName("email") val email: String,
    @SerializedName("profiles") val profiles: List<NextDnsProfile>
)

data class NextDnsProfile(
    @SerializedName("id") val id: String,
    @SerializedName("fingerprint") val fingerprint: String,
    @SerializedName("role") val role: String,
    @SerializedName("name") val name: String
)

