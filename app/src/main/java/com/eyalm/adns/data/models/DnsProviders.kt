package com.eyalm.adns.data.models

object DnsProviders {

    val ADGUARD = DnsProvider.Standard(
        id = "adguard_default",
        name = "AdGuard DNS",
        description = "The public AdGuard DNS server. Blocks Ads and Trackers.",
        hostname = "dns.adguard-dns.com"
    )

    val GOOGLE = DnsProvider.Standard(
        id = "google",
        name = "Google DNS",
        description = "The public Google DNS server.",
        hostname = "dns.google"

    )

    val CLOUDFLARE = DnsProvider.Standard(
        id = "cloudflare",
        name = "Cloudflare DNS",
        description = "The public Cloudflare DNS server.",
        hostname = "cloudflare-dns.com"
    )

    val NEXTDNS = DnsProvider.Enhanced(
        id = "nextdns",
        name = "NextDNS",
        description = "Connect your account to use NextDNS as a DNS provider.",
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