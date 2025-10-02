package com.mira.videoeditor.di

import android.content.Context
import androidx.room.Room
import com.mira.videoeditor.db.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database dependencies.
 * 
 * Provides Room database and DAOs for dependency injection.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.get(context)
    }

    @Provides
    fun provideVideoDao(database: AppDatabase): VideoDao = database.videoDao()

    @Provides
    fun provideShotDao(database: AppDatabase): ShotDao = database.shotDao()

    @Provides
    fun provideTextDao(database: AppDatabase): TextDao = database.textDao()

    @Provides
    fun provideEmbeddingDao(database: AppDatabase): EmbeddingDao = database.embeddingDao()

    @Provides
    fun provideReadModelsDao(database: AppDatabase): ReadModelsDao = database.readModelsDao()

    @Provides
    fun provideTextFtsDao(database: AppDatabase): TextFtsDao = database.textFtsDao()
}
