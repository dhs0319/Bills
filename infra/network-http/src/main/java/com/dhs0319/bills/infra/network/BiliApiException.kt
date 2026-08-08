package com.dhs0319.bills.infra.network

/**
 * B站 API 统一异常
 */
class BiliApiException(
    val code: Int,
    override val message: String
) : Exception("[$code] $message")
