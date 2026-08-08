package com.naaammme.bbspace.infra.player.di

import androidx.media3.common.util.UnstableApi
import com.naaammme.bbspace.infra.player.Media3PlayerEngine
import com.naaammme.bbspace.infra.player.PlayerEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
abstract class PlayerModule {
    @Binds
    @UnstableApi
    @Singleton
    abstract fun bindPlayerEngine(impl: Media3PlayerEngine): PlayerEngine
}
