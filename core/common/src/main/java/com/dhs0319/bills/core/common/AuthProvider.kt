package com.dhs0319.bills.core.common

/**
 * 登录态提供者
 * 网络层自动读取，调用方无需手动传递
 */
interface AuthProvider {
    val mid: Long
    val accessToken: String
}
