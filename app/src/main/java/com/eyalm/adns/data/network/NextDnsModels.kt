package com.eyalm.adns.data.network

import com.google.gson.annotations.SerializedName

data class NextDnsLoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("code") val code: String? = null
)

data class NextDnsLoginResponse(
    @SerializedName("requiresCode") val requiresCode: Boolean? = null
)

data class NextDnsCreateApiKeyResponse(
    @SerializedName("apiKey") val key: String
)

data class NextDnsProfileResponse(
    @SerializedName("email") val email: String,
    @SerializedName("profiles") val profiles: List<NextDnsProfile>
)

data class NextDnsProfilesResponse(
    @SerializedName("data") val data: List<NextDnsProfile>
)

data class NextDnsProfile(
    @SerializedName("id") val id: String,
    @SerializedName("fingerprint") val fingerprint: String? = null,
    @SerializedName("role") val role: String? = null,
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
    @SerializedName("meta") val meta: Any
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


data class NextDnsLogsResponse(
    @SerializedName("data") val data: List<NextDnsLogEntry>,
    @SerializedName("meta") val meta: NextDnsLogsMeta? = null
)

data class NextDnsLogEntry(
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("domain") val domain: String,
    @SerializedName("root") val root: String? = null,
    @SerializedName("tracker") val tracker: String? = null,
    @SerializedName("encrypted") val encrypted: Boolean,
    @SerializedName("protocol") val protocol: String,
    @SerializedName("clientIp") val clientIp: String? = null,
    @SerializedName("status") val status: String, // "default" | "error" | "blocked" | "allowed"
    @SerializedName("reasons") val reasons: List<NextDnsLogReason> = emptyList(),
    @SerializedName("device") val device: NextDnsLogDevice? = null,
    @SerializedName("type") val type: String? = null
)

data class NextDnsLogReason(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String
)

data class NextDnsLogDevice(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("model") val model: String?
)

data class NextDnsLogsMeta(
    @SerializedName("pagination") val pagination: NextDnsLogsPagination? = null
)

data class NextDnsLogsPagination(
    @SerializedName("cursor") val cursor: String? = null
)

data class NextDnsErrorResponse(
    @SerializedName("errors") val errors: List<NextDnsError> = emptyList()
)

data class NextDnsError(
    @SerializedName("code") val code: String
)

data class NextDnsDevicesResponse(
    @SerializedName("data") val data: List<NextDnsDeviceItem>
)
data class NextDnsDeviceItem(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String?,
    @SerializedName("queries") val queries: Int?
)


fun String.toHexId(): String {
    val hex = this.toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) }
    return "hex:$hex"
}