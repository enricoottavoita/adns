package com.eyalm.adns.data.models
import com.eyalm.adns.R


sealed class DnsProvider {

    abstract val id: String
    abstract val nameRes: Int
    abstract val descriptionRes: Int
    abstract val isEnhanced: Boolean

    data class Standard(
        override val id: String,
        override val nameRes: Int,
        override val descriptionRes: Int,
        val hostname: String      //"dns.adguard-dns.com" etc
    ) : DnsProvider() {
        override val isEnhanced = false
    }

    data class Enhanced(
        override val id: String,
        override val nameRes: Int,
        override val descriptionRes: Int,
    ) : DnsProvider() {
        override val isEnhanced = true
    }


    data class Custom(
        val userUrl: String
    ) : DnsProvider() {
        override val id = "custom"
        override val nameRes = R.string.custom_dns_hostname
        override val descriptionRes = R.string.use_a_custom_dns_hostname
        override val isEnhanced = false
    }
}


