package com.mira.whisper

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import android.util.Log

/**
 * Activity for Whisper Processing UI.
 * 
 * Loads the whisper-processing.html WebView with AndroidWhisper bridge
 * and integrates with WhisperConnectorService for real-time updates.
 */
class WhisperProcessingActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "WhisperProcessingActivity"
    }
    
    private lateinit var webView: WebView
    private lateinit var connectorReceiver: WhisperConnectorReceiver
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "Creating WhisperProcessingActivity")
        
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "Page finished loading: $url")
                    
                    // Initialize connector service integration
                    initializeConnectorService()
                }
                
                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Log.e(TAG, "WebView error: $description ($errorCode) for $failingUrl")
                }
            }
            
            // Add the WhisperBridge
            addJavascriptInterface(AndroidWhisperBridge(this@WhisperProcessingActivity), "WhisperBridge")
            
            // Load the processing page (Step 2 of 3-page flow)
            loadUrl("file:///android_asset/web/whisper_processing.html")
        }
        
        setContentView(webView)
        
        // Initialize connector receiver
        connectorReceiver = WhisperConnectorReceiver(webView, "processing")
    }
    
    /**
     * Initialize connector service integration
     */
    private fun initializeConnectorService() {
        try {
            // Start the connector service
            val connectorIntent = Intent(this, WhisperConnectorService::class.java)
            startService(connectorIntent)
            
            // Register broadcast receiver for real-time updates
            val filter = IntentFilter().apply {
                addAction(WhisperConnectorService.ACTION_START_PROCESSING)
                addAction(WhisperConnectorService.ACTION_UPDATE_PROGRESS)
                addAction(WhisperConnectorService.ACTION_PROCESSING_COMPLETE)
                addAction(WhisperConnectorService.ACTION_RESOURCE_UPDATE)
                addAction(WhisperConnectorService.ACTION_PAGE_NAVIGATION)
            }
            registerReceiver(connectorReceiver, filter)
            
            Log.d(TAG, "Connector service initialized")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing connector service: ${e.message}", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Unregister receiver
        try {
            unregisterReceiver(connectorReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering connector receiver: ${e.message}")
        }
        Log.d(TAG, "Destroying WhisperProcessingActivity")
    }
}
