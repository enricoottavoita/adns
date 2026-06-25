package com.eyalm.adns.data
import com.eyalm.adns.R
import android.content.Context


import androidx.compose.ui.graphics.vector.ImageVector
import com.google.gson.JsonObject

data class ToggleSetting(
    val apiPath: List<String>,
    val localeKey: String,
    val category: String,
    val customTitleRes: Int? = null,
    val customDescriptionRes: Int? = null
) {
    // for simple (non-nested) toggles

    constructor(
        apiKey: String,
        localeKey: String,
        category: String
    ) : this(
        apiPath = listOf(apiKey),
        localeKey = localeKey,
        category = category
    )

    val stateKey: String get() = apiPath.joinToString(".")

    fun title(context: Context): String =
        customTitleRes?.let { context.getString(it) } ?: Locales.getString(category, localeKey, "name")

    fun description(context: Context): String =
        customDescriptionRes?.let { context.getString(it) } ?: Locales.getString(category, localeKey, "description")



    // get this toggle's boolean value from a response
    fun readFrom(data: JsonObject): Boolean? {
        var current: JsonObject = data
        for (i in 0 until apiPath.size - 1) {
            current = current.getAsJsonObject(apiPath[i]) ?: return null
        }
        val element = current.get(apiPath.last()) ?: return null
        return if (element.isJsonPrimitive && element.asJsonPrimitive.isBoolean) {
            element.asBoolean
        } else null
    }

    fun buildPatchPayload(value: Boolean): Map<String, Any> {
        var result: Map<String, Any> = mapOf(apiPath.last() to value)
        for (i in apiPath.size - 2 downTo 0) {
            result = mapOf(apiPath[i] to result)
        }
        return result
    }
}



sealed class ListIcon {
    data class Url(val url: String) : ListIcon()
    data class Vector(val imageVector: ImageVector) : ListIcon()
    data class Text(val text: String) : ListIcon()
    object None : ListIcon()
}

data class ListItem(
    val id: String,
    val name: String,
    val description: String? = null,
    val icon: ListIcon = ListIcon.None
)


enum class ListSource {
    SERVER,
    LOCALE
}

data class ListSetting(
    val apiPage: String,         // API path segment: "security", "privacy", "parentalcontrol"
    val apiFeat: String,         // API path segment: "tlds", "blocklists", "natives", etc.
    val localeCategory: String,  // Category in merged.json
    val localeKey: String,       // Key in merged.json
    val source: ListSource,      // Where available items come from
    val localePath: List<String>, // Path in merged.json for locale-sourced lists
    val parentPage: Page? = null,       // Which page to go back to
    val customTitleRes: Int? = null,
    val customDescriptionRes: Int? = null,
    val customDescription: String? = null,
    val allowsCustomInput: Boolean = false
) {
    enum class Page { SECURITY, PRIVACY, PARENTAL_CONTROL }

    fun title(context: Context): String = customTitleRes?.let { context.getString(it) } ?: Locales.getString(localeCategory, localeKey, "name")
    fun description(context: Context): String = customDescriptionRes?.let { context.getString(it) } ?: customDescription ?: Locales.getString(localeCategory, localeKey, "description")

}


object SecuritySettings {
    val toggles = listOf(
        ToggleSetting("threatIntelligenceFeeds", "feeds",             "security"),
        ToggleSetting("aiThreatDetection",       "ai",                "security"),
        ToggleSetting("googleSafeBrowsing",      "googleSafeBrowsing","security"),
        ToggleSetting("cryptojacking",           "cryptojacking",     "security"),
        ToggleSetting("dnsRebinding",            "dnsRebinding",      "security"),
        ToggleSetting("idnHomographs",           "homograph",         "security"),
        ToggleSetting("typosquatting",           "typosquatting",     "security"),
        ToggleSetting("dga",                     "dga",               "security"),
        ToggleSetting("nrd",                     "nrd",               "security"),
        ToggleSetting("ddns",                    "ddns",              "security"),
        ToggleSetting("parking",                 "parked",            "security"),
        ToggleSetting("csam",                    "csam",              "security"),
    )
    val lists = listOf(
        ListSetting(
            apiPage = "security",
            apiFeat = "tlds",
            localeCategory = "security",
            localeKey = "tld",
            source = ListSource.SERVER,
            localePath = emptyList(),
            parentPage = ListSetting.Page.SECURITY
        )
    )
}


object PrivacySettings {
    val toggles = listOf(
        ToggleSetting("disguisedTrackers", "disguised",  "privacy"),
        ToggleSetting("allowAffiliate",    "affiliate",  "privacy"),
    )
    val lists = listOf(
        ListSetting(
            apiPage = "privacy",
            apiFeat = "blocklists",
            localeCategory = "privacy",
            localeKey = "blocklists",
            source = ListSource.SERVER,
            localePath = emptyList(),
            parentPage = ListSetting.Page.PRIVACY
        ),
        ListSetting(
            apiPage = "privacy",
            apiFeat = "natives",
            localeCategory = "privacy",
            localeKey = "native",
            source = ListSource.LOCALE,
            localePath = listOf("privacy", "native", "systems"),
            parentPage = ListSetting.Page.PRIVACY
        ),
    )
}

object ParentalControlSettings {
    val toggles = listOf(
        ToggleSetting("safeSearch",            "safesearch",         "parentalControl"),
        ToggleSetting("youtubeRestrictedMode", "youtubeRestricted",  "parentalControl"),
        ToggleSetting("blockBypass",           "bypass",             "parentalControl"),
    )
    val lists = listOf(
        ListSetting(
            apiPage = "parentalcontrol",
            apiFeat = "services",
            localeCategory = "parentalControl",
            localeKey = "services",
            source = ListSource.SERVER,
            localePath = listOf("parentalControl", "services", "services"),
            parentPage = ListSetting.Page.PARENTAL_CONTROL
        ),
        ListSetting(
            apiPage = "parentalcontrol",
            apiFeat = "categories",
            localeCategory = "parentalControl",
            localeKey = "categories",
            source = ListSource.LOCALE,
            localePath = listOf("parentalControl", "categories", "categories"),
            parentPage = ListSetting.Page.PARENTAL_CONTROL
        ),
    )
}

object SettingsPageSettings {
    val toggles = listOf(

        ToggleSetting(
            apiPath = listOf("bav"),
            localeKey = "bav", category = "settings",
            customTitleRes = R.string.bypass_age_verification,
            customDescriptionRes = R.string.automatically_bypass_age_verification_checks_used_by_certain_websites_such_as_adult_content_sites
        ),
        ToggleSetting(
            apiPath = listOf("web3"),
            localeKey = "web3", category = "settings",
            customTitleRes = R.string.web3,
            customDescriptionRes = R.string.enable_web3_domain_resolution
        ),

        // toggles under "logs"
        // TODO add text input
        ToggleSetting(
            apiPath = listOf("logs", "enabled"),
            localeKey = "logs", category = "settings",
            customTitleRes = R.string.logs,
            customDescriptionRes = R.string.enable_or_disable_query_logging
        ),
        ToggleSetting(
            apiPath = listOf("logs", "drop", "ip"),
            localeKey = "logs", category = "settings",
            customTitleRes = R.string.drop_ip_from_logs,
            customDescriptionRes = R.string.strip_client_ip_addresses_from_all_log_entries
        ),
        ToggleSetting(
            apiPath = listOf("logs", "drop", "domain"),
            localeKey = "logs", category = "settings",
            customTitleRes = R.string.drop_domains_from_logs,
            customDescriptionRes = R.string.strip_domain_names_from_all_log_entries
        ),

        // toggles under "blockPage"
        ToggleSetting(
            apiPath = listOf("blockPage", "enabled"),
            localeKey = "blockPage", category = "settings",
            customTitleRes = R.string.block_page,
            customDescriptionRes = R.string.show_a_block_page_when_a_domain_is_blocked
        ),

        // toggles under "performance"
        ToggleSetting(
            apiPath = listOf("performance", "ecs"),
            localeKey = "performance", category = "settings",
            customTitleRes = R.string.edns_client_subnet,
            customDescriptionRes = R.string.helps_cdns_locate_you_more_accurately_for_faster_content_delivery
        ),
        ToggleSetting(
            apiPath = listOf("performance", "cacheBoost"),
            localeKey = "performance", category = "settings",
            customTitleRes = R.string.cache_boost,
            customDescriptionRes = R.string.boost_dns_performance_by_increasing_cache_ttls
        ),
        ToggleSetting(
            apiPath = listOf("performance", "cnameFlattening"),
            localeKey = "performance", category = "settings",
            customTitleRes = R.string.cname_flattening,
            customDescriptionRes = R.string.resolve_cnames_to_their_final_target_for_enhanced_security
        ),
    )

    val lists = emptyList<ListSetting>()
}

object DenyList {
    val toggles = emptyList<ToggleSetting>()
    val lists = listOf(
        ListSetting(
            apiPage = "denylist",
            apiFeat = "",
            localeCategory = "pages",
            localeKey = "denylist",
            source = ListSource.SERVER,
            localePath = emptyList(),
            customTitleRes = R.string.denylist,
            customDescription = Locales.getString("xlist", "denylist", "info"),
            allowsCustomInput = true
        )
    )
}

object Allowlist {
    val toggles = emptyList<ToggleSetting>()
    val lists = listOf(
        ListSetting(
            apiPage = "allowlist",
            apiFeat = "",
            localeCategory = "pages",
            localeKey = "allowlist",
            customTitleRes = R.string.allowlist,
            customDescription = Locales.getString("xlist", "allowlist", "info"),
            source = ListSource.SERVER,
            localePath = emptyList(),
            allowsCustomInput = true
        )
    )
}