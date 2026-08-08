package com.dhs0319.bills.infra.network.dns

import java.net.InetAddress

data class DnsResult(
    val addresses: List<InetAddress>,
    val ttlSeconds: Long
)
