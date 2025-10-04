package com.mira.whisper

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import java.util.Timer
import java.util.TimerTask

/**
 * Activity for Whisper Results UI.
 * 
 * Loads the whisper_results.html WebView with AndroidWhisper bridge.
 */
class WhisperResultsActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "WhisperResultsActivity"
    }
    
    private lateinit var webView: WebView
    private var resourceTimer: Timer? = null
    private lateinit var whisperBridge: AndroidWhisperBridge
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "Creating WhisperResultsActivity")
        
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "Page finished loading: $url")
                }
                
                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Log.e(TAG, "WebView error: $description ($errorCode) for $failingUrl")
                }
            }
            
            // Add the WhisperBridge
            whisperBridge = AndroidWhisperBridge(this@WhisperResultsActivity)
            addJavascriptInterface(whisperBridge, "WhisperBridge")
            
            // Load the whisper results HTML file
            loadUrl("file:///android_asset/web/whisper_results.html")
        }
        
        setContentView(webView)
        
        // Start resource monitoring
        startResourceMonitoring()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopResourceMonitoring()
        Log.d(TAG, "WhisperResultsActivity destroyed")
    }
    
    private fun startResourceMonitoring() {
        resourceTimer = Timer()
        resourceTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    updateResourceUsage()
                }
            }
        }, 0, 2000) // Update every 2 seconds
        Log.d(TAG, "Resource monitoring started")
    }
    
    private fun stopResourceMonitoring() {
        resourceTimer?.cancel()
        resourceTimer = null
        Log.d(TAG, "Resource monitoring stopped")
    }
    
    private fun updateResourceUsage() {
        try {
            val resourceStats = whisperBridge.getResourceStats()
            
            // Send resource stats to WebView
            webView.evaluateJavascript("""
                if (typeof updateResourceStats === 'function') {
                    updateResourceStats('$resourceStats');
                }
            """.trimIndent(), null)
            
            Log.d(TAG, "Resource stats updated: $resourceStats")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating resource usage: ${e.message}")
        }
    }
}
