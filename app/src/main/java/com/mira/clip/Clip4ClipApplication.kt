package com.mira.clip

import android.app.Application
import android.util.Log

/**
 * Simple Application class for mira_clip CLIP4Clip service.
 * 
 * Minimal implementation without dependency injection for now.
 */
class Clip4ClipApplication : Application() {
    
    companion object {
        private const val TAG = "Clip4ClipApplication"
        lateinit var instance: Clip4ClipApplication
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Set system properties early to prevent WebView crashes
        try {
            System.setProperty("webview.disable-vulkan", "true")
            System.setProperty("webview.disable-gpu", "true")
            System.setProperty("webview.force-software-rendering", "true")
            Log.i(TAG, "Disabled Vulkan and GPU acceleration globally")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set system properties: ${e.message}", e)
        }
        
        // Initialize CLIP4Clip service components
        initializeClip4ClipService()
    }
    
    /**
     * Initialize CLIP4Clip service components.
     */
    private fun initializeClip4ClipService() {
        // TODO: Initialize core components when ready
    }
}
