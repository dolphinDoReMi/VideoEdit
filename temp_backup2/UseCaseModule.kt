package com.mira.videoeditor.di

import android.content.Context
import com.mira.videoeditor.data.SettingsRepository
import com.mira.videoeditor.usecases.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for use case dependencies.
 * 
 * Provides use cases for the presentation layer.
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideVideoIngestionUseCase(
        @ApplicationContext context: Context
    ): VideoIngestionUseCase = VideoIngestionUseCase(context)

    @Provides
    @Singleton
    fun provideVideoSearchUseCase(
        @ApplicationContext context: Context
    ): VideoSearchUseCase = VideoSearchUseCase(context)

    @Provides
    @Singleton
    fun provideDatabaseManagementUseCase(
        @ApplicationContext context: Context
    ): DatabaseManagementUseCase = DatabaseManagementUseCase(context)

    @Provides
    @Singleton
    fun provideSettingsUseCase(
        settingsRepository: SettingsRepository
    ): SettingsUseCase = SettingsUseCase(settingsRepository)
}
