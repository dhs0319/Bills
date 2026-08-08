package com.dhs0319.bills.core.auth

import com.dhs0319.bills.core.common.AuthProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthProviderImpl @Inject constructor(
    private val authStore: AuthStore
) : AuthProvider {
    override val mid: Long get() = authStore.mid
    override val accessToken: String get() = authStore.accessToken
}
