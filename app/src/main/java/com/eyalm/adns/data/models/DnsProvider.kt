package com.eyalm.adns.data.models

import com.eyalm.adns.data.network.NextDnsProfile

sealed class DnsProvider {

    abstract val id: String
    abstract val name: String
    abstract val description: String
    abstract val isEnhanced: Boolean

    data class Standard(
        override val id: String,
        override val name: String,
        override val description: String,
        val hostname: String      //"dns.adguard-dns.com" etc
    ) : DnsProvider() {
        override val isEnhanced = false
    }

    data class Enhanced(
        override val id: String,
        override val name: String,
        override val description: String,
        var currentProfile: NextDnsProfile?
    ) : DnsProvider() {
        override val isEnhanced = true
    }


    data class Custom(
        val userUrl: String
    ) : DnsProvider() {
        override val id = "custom"
        override val name = "Custom DNS Hostname"
        override val description = "Use a custom DNS hostname"
        override val isEnhanced = false
    }
}


