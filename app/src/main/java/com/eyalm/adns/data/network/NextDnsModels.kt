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

data class NextDnsAnalytics(
    @SerializedName("data") val data: List<NextDnsAnalyticsData>,
    @SerializedName("meta") val meta: Any
)

data class NextDnsAnalyticsData(
    @SerializedName("status") val status: String,
    @SerializedName("queries") val queries: String,
)

// Request for creating a new profile
data class NextDnsCreateProfileRequest(
    @SerializedName("name") val name: String,
    @SerializedName("security") val security: Map<String, Boolean> = mapOf(
        "threatIntelligenceFeeds" to true,
        "googleSafeBrowsing" to true,
        "cryptojacking" to true,
        "idnHomographs" to true,
        "typosquatting" to true,
        "dga" to true,
        "csam" to true
    ),
    @SerializedName("privacy") val privacy: Map<String, Any> = mapOf(
        "blocklists" to listOf(mapOf("id" to "nextdns-recommended")),
        "disguisedTrackers" to true
    ),
    @SerializedName("settings") val settings: Map<String, Map<String, Boolean>> = mapOf(
        "logs" to mapOf("enabled" to true),
        "performance" to mapOf("ecs" to true)
    )
) {
    companion object {
        fun withName(name: String) = NextDnsCreateProfileRequest(name = name)
    }
}

data class NextDnsBlocklistResponse(
    @SerializedName("data") val data: List<NextDnsBlocklistData>
)

data class NextDnsBlocklistData(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("website") val website: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("entries") val entries: Int,
    @SerializedName("updatedOn") val updatedOn: String
)

data class NextDnsPrivacyResponse(
    @SerializedName("data") val data: NextDnsPrivacyData
)

data class NextDnsPrivacyData(
    @SerializedName("allowAffiliate") val allowAffiliate: Boolean,
    @SerializedName("blocklists") val blocklists: List<NextDnsBlocklistData>,
    @SerializedName("disguisedTrackers") val disguisedTrackers: Boolean,
    @SerializedName("natives") val natives: List<Any>?
)

data class NextDnsUpdateBlocklistsRequest(
    @SerializedName("id") val id: String
)


// https://api.nextdns.io/profiles/***/analytics/status;series?from=-30m&alignment=start&timezone=Asia%2FJerusalem

data class NextDnsStatsGraphResponse(
    @SerializedName("data") val data: List<NextDnsStatsGraphData>,
    @SerializedName("meta") val meta: Any // TODO if needed
)

data class NextDnsStatsGraphData(
    @SerializedName("queries") val queries: List<Int>,
    @SerializedName("status") val status: String
)


// https://api.nextdns.io/profiles/*****/analytics/domains?status=default%2Callowed&from=-30m&limit=6
// https://api.nextdns.io/profiles/*****/analytics/domains?status=blocked&from=-30m&limit=6

data class NextDnsDomainsResponse(
    @SerializedName("data") val data: List<NextDnsDomainData>,
    @SerializedName("meta") val meta: Any
)

data class NextDnsDomainData(
    @SerializedName("domain") val domain: String,
    @SerializedName("queries") val queries: Int
)


fun String.toHexId(): String {
    val hex = this.toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) }
    return "hex:$hex"
}