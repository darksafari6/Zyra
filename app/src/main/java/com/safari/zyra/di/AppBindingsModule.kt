package com.safari.zyra.di

import com.safari.zyra.core.data.SettingsDataStore
import com.safari.zyra.core.data.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingsModule {
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(implementation: SettingsDataStore): SettingsRepository
}
