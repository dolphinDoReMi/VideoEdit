package com.mira.videoeditor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing app settings and preferences.
 * 
 * Handles storage of embedding variants, processing preferences,
 * and other configuration data using DataStore.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    
    companion object {
        // Preference keys
        private val CURRENT_VARIANT = stringPreferencesKey("current_embedding_variant")
        private val DEFAULT_FRAMES_PER_VIDEO = intPreferencesKey("default_frames_per_video")
        private val DEFAULT_FRAMES_PER_SHOT = intPreferencesKey("default_frames_per_shot")
        private val ENABLE_SHOT_EMBEDDINGS = booleanPreferencesKey("enable_shot_embeddings")
        private val ENABLE_BACKGROUND_PROCESSING = booleanPreferencesKey("enable_background_processing")
        private val MAX_CONCURRENT_WORKERS = intPreferencesKey("max_concurrent_workers")
        private val ENABLE_PERFORMANCE_MONITORING = booleanPreferencesKey("enable_performance_monitoring")
        private val DATABASE_ENCRYPTION_ENABLED = booleanPreferencesKey("database_encryption_enabled")
        
        // Default values
        private const val DEFAULT_VARIANT = "clip_vit_b32_mean_v1"
        private const val DEFAULT_FRAMES_PER_VIDEO_VALUE = 32
        private const val DEFAULT_FRAMES_PER_SHOT_VALUE = 12
        private const val DEFAULT_MAX_WORKERS = 2
    }

    /**
     * Get the current embedding variant.
     */
    val currentVariant: Flow<String> = dataStore.data.map { preferences ->
        preferences[CURRENT_VARIANT] ?: DEFAULT_VARIANT
    }

    /**
     * Set the current embedding variant.
     */
    suspend fun setCurrentVariant(variant: String) {
        dataStore.edit { preferences ->
            preferences[CURRENT_VARIANT] = variant
        }
    }

    /**
     * Get default frames per video setting.
     */
    val defaultFramesPerVideo: Flow<Int> = dataStore.data.map { preferences ->
        preferences[DEFAULT_FRAMES_PER_VIDEO] ?: DEFAULT_FRAMES_PER_VIDEO_VALUE
    }

    /**
     * Set default frames per video setting.
     */
    suspend fun setDefaultFramesPerVideo(frames: Int) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_FRAMES_PER_VIDEO] = frames
        }
    }

    /**
     * Get default frames per shot setting.
     */
    val defaultFramesPerShot: Flow<Int> = dataStore.data.map { preferences ->
        preferences[DEFAULT_FRAMES_PER_SHOT] ?: DEFAULT_FRAMES_PER_SHOT_VALUE
    }

    /**
     * Set default frames per shot setting.
     */
    suspend fun setDefaultFramesPerShot(frames: Int) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_FRAMES_PER_SHOT] = frames
        }
    }

    /**
     * Check if shot embeddings are enabled.
     */
    val enableShotEmbeddings: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ENABLE_SHOT_EMBEDDINGS] ?: true
    }

    /**
     * Enable or disable shot embeddings.
     */
    suspend fun setEnableShotEmbeddings(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ENABLE_SHOT_EMBEDDINGS] = enabled
        }
    }

    /**
     * Check if background processing is enabled.
     */
    val enableBackgroundProcessing: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ENABLE_BACKGROUND_PROCESSING] ?: true
    }

    /**
     * Enable or disable background processing.
     */
    suspend fun setEnableBackgroundProcessing(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ENABLE_BACKGROUND_PROCESSING] = enabled
        }
    }

    /**
     * Get maximum concurrent workers setting.
     */
    val maxConcurrentWorkers: Flow<Int> = dataStore.data.map { preferences ->
        preferences[MAX_CONCURRENT_WORKERS] ?: DEFAULT_MAX_WORKERS
    }

    /**
     * Set maximum concurrent workers.
     */
    suspend fun setMaxConcurrentWorkers(maxWorkers: Int) {
        dataStore.edit { preferences ->
            preferences[MAX_CONCURRENT_WORKERS] = maxWorkers
        }
    }

    /**
     * Check if performance monitoring is enabled.
     */
    val enablePerformanceMonitoring: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ENABLE_PERFORMANCE_MONITORING] ?: false
    }

    /**
     * Enable or disable performance monitoring.
     */
    suspend fun setEnablePerformanceMonitoring(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ENABLE_PERFORMANCE_MONITORING] = enabled
        }
    }

    /**
     * Check if database encryption is enabled.
     */
    val databaseEncryptionEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DATABASE_ENCRYPTION_ENABLED] ?: false
    }

    /**
     * Enable or disable database encryption.
     */
    suspend fun setDatabaseEncryptionEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DATABASE_ENCRYPTION_ENABLED] = enabled
        }
    }

    /**
     * Get all settings as a map for debugging/logging.
     */
    suspend fun getAllSettings(): Map<String, Any> {
        return dataStore.data.map { preferences ->
            mapOf(
                "currentVariant" to (preferences[CURRENT_VARIANT] ?: DEFAULT_VARIANT),
                "defaultFramesPerVideo" to (preferences[DEFAULT_FRAMES_PER_VIDEO] ?: DEFAULT_FRAMES_PER_VIDEO_VALUE),
                "defaultFramesPerShot" to (preferences[DEFAULT_FRAMES_PER_SHOT] ?: DEFAULT_FRAMES_PER_SHOT_VALUE),
                "enableShotEmbeddings" to (preferences[ENABLE_SHOT_EMBEDDINGS] ?: true),
                "enableBackgroundProcessing" to (preferences[ENABLE_BACKGROUND_PROCESSING] ?: true),
                "maxConcurrentWorkers" to (preferences[MAX_CONCURRENT_WORKERS] ?: DEFAULT_MAX_WORKERS),
                "enablePerformanceMonitoring" to (preferences[ENABLE_PERFORMANCE_MONITORING] ?: false),
                "databaseEncryptionEnabled" to (preferences[DATABASE_ENCRYPTION_ENABLED] ?: false)
            )
        }.let { flow -> 
            // Convert Flow to suspend function result
            flow.map { it }.let { it }
        }
    }

    /**
     * Reset all settings to defaults.
     */
    suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
