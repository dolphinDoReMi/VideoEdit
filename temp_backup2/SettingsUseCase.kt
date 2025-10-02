package com.mira.videoeditor.usecases

import com.mira.videoeditor.data.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for managing app settings and preferences.
 * 
 * Provides high-level operations for settings management,
 * encapsulating business logic for configuration changes.
 */
class SettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {

    /**
     * Get the current embedding variant.
     */
    fun getCurrentVariant(): Flow<String> = settingsRepository.currentVariant

    /**
     * Set the current embedding variant.
     */
    suspend fun setCurrentVariant(variant: String) {
        settingsRepository.setCurrentVariant(variant)
    }

    /**
     * Get default frames per video setting.
     */
    fun getDefaultFramesPerVideo(): Flow<Int> = settingsRepository.defaultFramesPerVideo

    /**
     * Set default frames per video setting.
     */
    suspend fun setDefaultFramesPerVideo(frames: Int) {
        settingsRepository.setDefaultFramesPerVideo(frames)
    }

    /**
     * Get default frames per shot setting.
     */
    fun getDefaultFramesPerShot(): Flow<Int> = settingsRepository.defaultFramesPerShot

    /**
     * Set default frames per shot setting.
     */
    suspend fun setDefaultFramesPerShot(frames: Int) {
        settingsRepository.setDefaultFramesPerShot(frames)
    }

    /**
     * Check if shot embeddings are enabled.
     */
    fun isShotEmbeddingsEnabled(): Flow<Boolean> = settingsRepository.enableShotEmbeddings

    /**
     * Enable or disable shot embeddings.
     */
    suspend fun setShotEmbeddingsEnabled(enabled: Boolean) {
        settingsRepository.setEnableShotEmbeddings(enabled)
    }

    /**
     * Check if background processing is enabled.
     */
    fun isBackgroundProcessingEnabled(): Flow<Boolean> = settingsRepository.enableBackgroundProcessing

    /**
     * Enable or disable background processing.
     */
    suspend fun setBackgroundProcessingEnabled(enabled: Boolean) {
        settingsRepository.setEnableBackgroundProcessing(enabled)
    }

    /**
     * Get maximum concurrent workers setting.
     */
    fun getMaxConcurrentWorkers(): Flow<Int> = settingsRepository.maxConcurrentWorkers

    /**
     * Set maximum concurrent workers.
     */
    suspend fun setMaxConcurrentWorkers(maxWorkers: Int) {
        settingsRepository.setMaxConcurrentWorkers(maxWorkers)
    }

    /**
     * Check if performance monitoring is enabled.
     */
    fun isPerformanceMonitoringEnabled(): Flow<Boolean> = settingsRepository.enablePerformanceMonitoring

    /**
     * Enable or disable performance monitoring.
     */
    suspend fun setPerformanceMonitoringEnabled(enabled: Boolean) {
        settingsRepository.setEnablePerformanceMonitoring(enabled)
    }

    /**
     * Check if database encryption is enabled.
     */
    fun isDatabaseEncryptionEnabled(): Flow<Boolean> = settingsRepository.databaseEncryptionEnabled

    /**
     * Enable or disable database encryption.
     */
    suspend fun setDatabaseEncryptionEnabled(enabled: Boolean) {
        settingsRepository.setDatabaseEncryptionEnabled(enabled)
    }

    /**
     * Get all settings for debugging/logging.
     */
    suspend fun getAllSettings(): Map<String, Any> {
        return settingsRepository.getAllSettings()
    }

    /**
     * Reset all settings to defaults.
     */
    suspend fun resetToDefaults() {
        settingsRepository.resetToDefaults()
    }
}
