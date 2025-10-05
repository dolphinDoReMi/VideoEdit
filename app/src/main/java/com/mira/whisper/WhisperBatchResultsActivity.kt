package com.mira.whisper

import android.content.Context.RECEIVER_NOT_EXPORTED
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.content.BroadcastReceiver
import android.content.Context
import java.util.Timer
import java.util.TimerTask

/**
 * Activity for Whisper Batch Results UI with table view.
 * 
 * Loads the whisper_batch_results.html WebView with AndroidWhisper bridge
 * and displays transcripts in a table format with metadata.
 */
class WhisperBatchResultsActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "WhisperBatchResultsActivity"
    }
    
    private lateinit var webView: WebView
    private var resourceTimer: Timer? = null
    private lateinit var whisperBridge: AndroidWhisperBridge
    private lateinit var connectorReceiver: WhisperConnectorReceiver
    private var batchId: String? = null
    private var debugExportReceiver: BroadcastReceiver? = null
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "Creating WhisperBatchResultsActivity")
        
        // Get batch ID from intent
        batchId = intent.getStringExtra("batchId")
        Log.d(TAG, "Batch ID: $batchId")
        
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "Page finished loading: $url")
                    
                    // Pass batch ID to the page
                    if (batchId != null) {
                        evaluateJavascript("""
                            if (typeof setBatchId === 'function') {
                                setBatchId('$batchId');
                            }
                        """, null)
                    }
                    
                    // Initialize connector service integration
                    initializeConnectorService()
                }
                
                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Log.e(TAG, "WebView error: $description ($errorCode) for $failingUrl")
                }
            }
            
            // Add the WhisperBridge
            whisperBridge = AndroidWhisperBridge(this@WhisperBatchResultsActivity)
            addJavascriptInterface(whisperBridge, "WhisperBridge")
            
            // Load the batch results page
            loadUrl("file:///android_asset/web/whisper_batch_results.html")
        }
        
        setContentView(webView)
        
        // Initialize connector receiver
        connectorReceiver = WhisperConnectorReceiver(webView, "batch_results")
        
        // Start resource monitoring (fallback)
        startResourceMonitoring()

        // Register debug export receiver to trigger CSV/JSON export from WebView
        registerDebugExportReceiver()
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
            registerReceiver(connectorReceiver, filter, RECEIVER_NOT_EXPORTED)
            
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
        
        stopResourceMonitoring()
        Log.d(TAG, "WhisperBatchResultsActivity destroyed")

        // Unregister debug export receiver
        try {
            if (debugExportReceiver != null) unregisterReceiver(debugExportReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering debug export receiver: ${e.message}")
        }
    }

    private fun registerDebugExportReceiver() {
        try {
            debugExportReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == "com.mira.whisper.DEBUG_EXPORT") {
                        Log.d(TAG, "Received DEBUG_EXPORT broadcast - triggering WebView export")
                        webView.evaluateJavascript(
                            """
                            if (window.triggerExport) { window.triggerExport(); }
                            """.trimIndent(),
                            null
                        )
                    }
                }
            }
            val filter = IntentFilter().apply { addAction("com.mira.whisper.DEBUG_EXPORT") }
            registerReceiver(debugExportReceiver, filter, RECEIVER_NOT_EXPORTED)
            Log.d(TAG, "Debug export receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register debug export receiver: ${e.message}", e)
        }
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
