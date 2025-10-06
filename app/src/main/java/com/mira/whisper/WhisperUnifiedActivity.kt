package com.mira.whisper

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Unified Whisper Processing Activity
 * 
 * This activity provides a single-page interface that combines all three steps
 * of the whisper processing pipeline into one comprehensive flow:
 * 
 * 1. File Selection & Configuration
 * 2. Real-time Processing & Monitoring  
 * 3. Results Display & Export
 * 
 * The interface dynamically shows/hides sections based on the current processing state,
 * providing a seamless user experience without page navigation.
 */
class WhisperUnifiedActivity : Activity() {
    
    companion object {
        private const val TAG = "WhisperUnified"
    }
    
    private lateinit var webView: WebView
    private lateinit var bridge: AndroidWhisperBridge
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.i(TAG, "Whisper Unified Activity launched - initializing interface")
        
        // Initialize WebView
        webView = WebView(this)
        setContentView(webView)
        
        // Configure WebView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
        }
        
        // Initialize bridge
        bridge = AndroidWhisperBridge(this)
        
        // Set the activity reference in bridge for file picker
        bridge.setActivity(this)
        
        // Add JavaScript bridge for whisper functionality
        webView.addJavascriptInterface(bridge, "WhisperBridge")
        
        // Set WebView client
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.i(TAG, "Whisper unified page loaded: $url")
                
                // Inject any necessary JavaScript after page load
                injectInitializationScript()
            }
        }
        
        // Load the unified interface
        webView.loadUrl("file:///android_asset/web/whisper_unified.html")
        
        Log.i(TAG, "Whisper unified interface initialized")
    }
    
    /**
     * Inject initialization script to set up the interface
     */
    private fun injectInitializationScript() {
        val script = """
            // Initialize the unified interface
            if (typeof initializeUnifiedInterface === 'function') {
                initializeUnifiedInterface();
            }
            
            // Set up real-time monitoring if available
            if (window.WhisperBridge && window.WhisperBridge.startResourceMonitoring) {
                window.WhisperBridge.startResourceMonitoring();
            }
        """.trimIndent()
        
        webView.evaluateJavascript(script, null)
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up resources
        if (::webView.isInitialized) {
            webView.destroy()
        }
        
        Log.i(TAG, "Whisper unified activity destroyed")
    }
    
    /**
     * Handle configuration changes
     */
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        
        // Notify WebView of configuration change
        webView.evaluateJavascript("""
            if (typeof handleConfigurationChange === 'function') {
                handleConfigurationChange();
            }
        """.trimIndent(), null)
    }
    
    /**
     * Handle memory pressure
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        // Notify the interface about memory pressure
        webView.evaluateJavascript("""
            if (typeof handleMemoryPressure === 'function') {
                handleMemoryPressure($level);
            }
        """.trimIndent(), null)
        
        Log.w(TAG, "Memory pressure detected: level $level")
    }
}
