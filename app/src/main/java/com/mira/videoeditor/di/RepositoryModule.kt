package com.mira.videoeditor.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.mira.videoeditor.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repository and data layer dependencies.
 * 
 * Provides repositories, settings, and data access components.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        dataStore: DataStore<Preferences>
    ): SettingsRepository = SettingsRepository(dataStore)

    @Provides
    @Singleton
    fun provideIngestionRepository(
        videoDao: com.mira.videoeditor.db.VideoDao,
        shotDao: com.mira.videoeditor.db.ShotDao,
        embeddingDao: com.mira.videoeditor.db.EmbeddingDao,
        @ApplicationContext context: Context
    ): IngestionRepository = IngestionRepository(videoDao, shotDao, embeddingDao, context)

    @Provides
    @Singleton
    fun provideRetrievalRepository(
        readDao: com.mira.videoeditor.db.ReadModelsDao,
        embeddingDao: com.mira.videoeditor.db.EmbeddingDao,
        textDao: com.mira.videoeditor.db.TextDao,
        @ApplicationContext context: Context
    ): RetrievalRepository = RetrievalRepository(readDao, embeddingDao, textDao, context)

    @Provides
    @Singleton
    fun providePerformanceMonitor(): PerformanceMonitor = PerformanceMonitor()
}
