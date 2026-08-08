package com.dhs0319.bills.core.auth.di

import com.dhs0319.bills.core.auth.AuthProviderImpl
import com.dhs0319.bills.core.common.AuthProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthProvider(impl: AuthProviderImpl): AuthProvider
}
