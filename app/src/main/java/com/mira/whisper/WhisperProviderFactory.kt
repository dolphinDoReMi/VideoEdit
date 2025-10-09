package com.mira.whisper

import android.content.Context
import com.mira.videoeditor.infra.storage.WhisperProvider

/**
 * Factory for creating WhisperProvider instances.
 * 
 * This allows the app module to provide real Whisper functionality
 * to the infra-storage module through dependency injection.
 */
object WhisperProviderFactory {
    
    /**
     * Creates a WhisperProvider instance.
     * 
     * @param context Android context
     * @param useRealProvider Whether to use real Whisper implementation (default: true)
     * @return WhisperProvider instance
     */
    fun create(context: Context, useRealProvider: Boolean = true): WhisperProvider {
        return if (useRealProvider) {
            try {
                RealWhisperProvider().also {
                    android.util.Log.d("WhisperProviderFactory", "✅ Created RealWhisperProvider")
                }
            } catch (e: Exception) {
                android.util.Log.w("WhisperProviderFactory", "⚠️ Failed to create RealWhisperProvider, falling back to mock", e)
                com.mira.videoeditor.infra.storage.MockWhisperProvider()
            }
        } else {
            android.util.Log.d("WhisperProviderFactory", "📝 Created MockWhisperProvider")
            com.mira.videoeditor.infra.storage.MockWhisperProvider()
        }
    }
}
