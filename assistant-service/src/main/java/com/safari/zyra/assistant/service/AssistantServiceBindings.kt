package com.safari.zyra.assistant.service

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AssistantServiceBindings {
    @Binds
    @Singleton
    abstract fun bindAssistantServiceController(
        implementation: AndroidAssistantServiceController
    ): AssistantServiceController
}
