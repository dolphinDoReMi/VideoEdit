package com.mira.whisper

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import org.json.JSONObject
import java.util.Timer
import java.util.TimerTask

/**
 * Activity for Whisper Results UI.
 * 
 * Loads the whisper_results.html WebView with AndroidWhisper bridge
 * and integrates with WhisperConnectorService for real-time updates.
 */
class WhisperResultsActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "WhisperResultsActivity"
    }
    
    private lateinit var webView: WebView
    private var resourceTimer: Timer? = null
    private lateinit var whisperBridge: AndroidWhisperBridge
    private lateinit var connectorReceiver: WhisperConnectorReceiver
    private lateinit var resourceReceiver: ResourceUpdateReceiver
    
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
                    
                    // Initialize connector service integration
                    initializeConnectorService()
                }
                
                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Log.e(TAG, "WebView error: $description ($errorCode) for $failingUrl")
                }
            }
            
            // Add the WhisperBridge
            whisperBridge = AndroidWhisperBridge(this@WhisperResultsActivity)
            addJavascriptInterface(whisperBridge, "WhisperBridge")
            
            // Load the results page (Step 3 of 3-page flow)
            loadUrl("file:///android_asset/web/whisper_results.html")
        }
        
        setContentView(webView)
        
        // Initialize connector receiver
        connectorReceiver = WhisperConnectorReceiver(webView, "results")
        
        // Initialize resource update receiver for background service
        resourceReceiver = ResourceUpdateReceiver(
            onResourceUpdate = { resourceData ->
                // Send resource data to WebView
                webView.post {
                    webView.evaluateJavascript(
                        "if (window.updateGlobalResourceStats) { window.updateGlobalResourceStats('${resourceData.toString()}'); }",
                        null
                    )
                }
            },
            onCpuUpdate = { cpuUsage ->
                Log.d(TAG, "CPU update: ${cpuUsage}%")
            },
            onMemoryUpdate = { memoryUsage ->
                Log.d(TAG, "Memory update: ${memoryUsage}%")
            },
            onBatteryUpdate = { batteryLevel ->
                Log.d(TAG, "Battery update: ${batteryLevel}%")
            },
            onTemperatureUpdate = { temperature ->
                Log.d(TAG, "Temperature update: ${temperature}°C")
            }
        )
        resourceReceiver.register(this)
        
        // Start background resource monitoring service
        startBackgroundResourceMonitoring()
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
        
        // Unregister receivers
        try {
            unregisterReceiver(connectorReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering connector receiver: ${e.message}")
        }
        
        try {
            resourceReceiver.unregister(this)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering resource receiver: ${e.message}")
        }
        
        stopResourceMonitoring()
        Log.d(TAG, "WhisperResultsActivity destroyed")
    }
    
    /**
     * Start the background DeviceResourceService for resource monitoring
     */
    private fun startBackgroundResourceMonitoring() {
        try {
            Log.d(TAG, "Starting DeviceResourceService for background resource monitoring")
            val intent = Intent(this, DeviceResourceService::class.java)
            startForegroundService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting background resource monitoring: ${e.message}")
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
