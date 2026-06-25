package com.eyalm.adns.data.models
import com.eyalm.adns.R


object DnsProviders {

    val ADGUARD = DnsProvider.Standard(
        id = "adguard_default",
        nameRes = R.string.adguard_dns,
        descriptionRes = R.string.the_public_adguard_dns_server_blocks_ads_and_trackers,
        hostname = "dns.adguard-dns.com"
    )

    val GOOGLE = DnsProvider.Standard(
        id = "google",
        nameRes = R.string.google_dns,
        descriptionRes = R.string.the_public_google_dns_server,
        hostname = "dns.google"

    )

    val CLOUDFLARE = DnsProvider.Standard(
        id = "cloudflare",
        nameRes = R.string.cloudflare_dns,
        descriptionRes = R.string.the_public_cloudflare_dns_server,
        hostname = "cloudflare-dns.com"
    )

    val NEXTDNS = DnsProvider.Enhanced(
        id = "nextdns",
        nameRes = R.string.nextdns_name,
        descriptionRes = R.string.connect_your_account_to_use_nextdns_as_a_dns_provider,
    )


    val getAllProviders = listOf(
        NEXTDNS,
        ADGUARD,
        GOOGLE,
        CLOUDFLARE
    )

    fun getProviderByHostname(hostname: String): DnsProvider {
        val matchedProvider = getAllProviders.find {
            it is DnsProvider.Standard && it.hostname == hostname
        }

        return matchedProvider ?: DnsProvider.Custom(hostname)
    }


}