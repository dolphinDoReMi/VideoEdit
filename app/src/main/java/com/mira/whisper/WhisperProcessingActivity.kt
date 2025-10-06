package com.mira.whisper

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.os.Build
import android.content.Context
import androidx.lifecycle.lifecycleScope
import com.mira.resource.DeviceResourceService
import com.mira.resource.ResourceUpdateReceiver
import com.mira.whisper.bus.WhisperBus
import com.mira.whisper.bus.WhisperEvent
import kotlinx.coroutines.launch
import org.json.JSONObject

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
    private var resourceReceiver: ResourceUpdateReceiver? = null
    private var lastResourceStatsJson: String? = null
    private var lastResourceUpdateMs: Long = 0L
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.e(TAG, "=== PROCESSING ACTIVITY CREATED ===")
        Log.e(TAG, "Intent extras: ${intent.extras}")
        Log.e(TAG, "Intent data: ${intent.data}")
        Log.e(TAG, "Intent action: ${intent.action}")
        Log.d(TAG, "Creating WhisperProcessingActivity")
        
        // Get batch ID from intent
        val batchId = intent.getStringExtra("batchId")
        Log.e(TAG, "Batch ID from intent: $batchId")
        
        // Load batch plan if batchId is provided
        val plan = batchId?.let { PlanStore.get(this, it) }
        Log.d(TAG, "Loaded plan for batch: $batchId, file count: ${plan?.uris?.size ?: 0}")
        
        val wv = WebView(this)
        webView = wv

        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.allowFileAccess = true
        wv.settings.allowContentAccess = true

        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page finished loading: $url")

                // Inject batch ID if provided
                batchId?.let { id ->
                    webView.evaluateJavascript("""
                        if (window.setBatchId) {
                            window.setBatchId('$id');
                        }
                    """, null)
                }

                // Initialize connector service integration
                initializeConnectorService()

                // Bind resource updates only after page is ready
                bindResourceUpdates()

                // Push latest cached stats if available
                lastResourceStatsJson?.let { latest ->
                    webView.evaluateJavascript(
                        """
                        if (window.updateResourceStats) {
                            window.updateResourceStats('$latest');
                        }
                        """.trimIndent(),
                        null
                    )
                }

                // Immediate fallback: fetch once via bridge to ensure first paint
                webView.evaluateJavascript(
                    """
                    (function(){
                      try {
                        if (window.WhisperBridge && WhisperBridge.getResourceStats && window.updateResourceStats) {
                          var s = WhisperBridge.getResourceStats();
                          if (s) { window.updateResourceStats(s); }
                        }
                      } catch (e) { console.log('init resource fetch error', e); }
                    })();
                    """.trimIndent(),
                    null
                )
            }

            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                Log.e(TAG, "WebView error: $description ($errorCode) for $failingUrl")
            }
        }

        // Add the WhisperBridge
        val bridge = AndroidWhisperBridge(this@WhisperProcessingActivity)
        Log.d(TAG, "Setting WebView on bridge...")
        bridge.setWebView(wv)
        Log.d(TAG, "WebView set on bridge, adding JavaScript interface...")
        wv.addJavascriptInterface(bridge, "WhisperBridge")

        // Load the processing page (Step 2 of 3-page flow)
        wv.loadUrl("file:///android_asset/web/whisper_processing.html")

        setContentView(wv)
        
        // Initialize connector receiver
        connectorReceiver = WhisperConnectorReceiver(webView, "processing")

        // Start Xiaomi background resource monitoring service
        startBackgroundResourceMonitoring()
        
        // Subscribe to WhisperBus events for real-time updates
        subscribeToWhisperEvents()
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(connectorReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(connectorReceiver, filter)
            }
            
            Log.d(TAG, "Connector service initialized")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing connector service: ${e.message}", e)
        }
    }

    /**
     * Start the Xiaomi device resource monitoring foreground service
     */
    private fun startBackgroundResourceMonitoring() {
        try {
            Log.d(TAG, "Starting DeviceResourceService for background resource monitoring")
            val intent = Intent(this, DeviceResourceService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            
            // Also start the connector service resource monitoring
            val connectorIntent = Intent(this, WhisperConnectorService::class.java)
            startService(connectorIntent)
            
            Log.d(TAG, "Both DeviceResourceService and WhisperConnectorService started for resource monitoring")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting background resource monitoring: ${e.message}")
        }
    }

    /**
     * Register a receiver to forward resource updates to the WebView
     */
    private fun bindResourceUpdates() {
        try {
            resourceReceiver = ResourceUpdateReceiver(
                onResourceUpdate = { resourceData ->
                    try {
                        val transformed = transformResourceDataForWeb(resourceData)
                        lastResourceStatsJson = transformed
                        lastResourceUpdateMs = System.currentTimeMillis()
                        webView.evaluateJavascript(
                            """
                            if (window.updateResourceStats) {
                                window.updateResourceStats('$transformed');
                            }
                            """.trimIndent(),
                            null
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error forwarding resource update: ${e.message}")
                    }
                }
            )
            resourceReceiver?.register(this)
            Log.d(TAG, "Resource update receiver bound")
        } catch (e: Exception) {
            Log.e(TAG, "Error binding resource updates: ${e.message}")
        }
    }

    /**
     * Transform service JSON to the structure expected by the WebView UI
     */
    private fun transformResourceDataForWeb(serviceJson: JSONObject): String {
        val memory = serviceJson.optDouble("memory", 0.0)
        val cpu = serviceJson.optDouble("cpu", 0.0)
        val batteryObj = serviceJson.optJSONObject("battery")
        val batteryLevel = batteryObj?.optInt("level") ?: serviceJson.optInt("battery", 0)
        val batteryTemp = batteryObj?.optDouble("temperature", 0.0) ?: 0.0
        val batteryVoltage = batteryObj?.optDouble("voltage", 0.0) ?: 0.0
        val batteryStatus = batteryObj?.optString("status", "Unknown") ?: "Unknown"
        val temperature = serviceJson.optDouble("temperature", 0.0)
        val gpuInfo = serviceJson.optString("gpu", "")
        val threads = serviceJson.optJSONObject("threads")
        val threadInfo = if (threads != null) {
            val count = threads.optInt("count", 0)
            val main = threads.optString("main_thread", "main")
            val active = threads.optInt("active_threads", 0)
            val cores = threads.optInt("cpu_cores", 0)
            "Threads: $count | Main: $main | Active: $active | CPUs: $cores"
        } else {
            ""
        }

        val out = JSONObject()
        out.put("memory", Math.round(memory))
        out.put("cpu", cpu)
        out.put("battery", batteryLevel)
        out.put("temperature", temperature)
        // Provide explicit thread count for the Processing Details section
        out.put("threadsCount", threads?.optInt("count", 0) ?: 0)
        out.put(
            "batteryDetails",
            "Level: ${batteryLevel}% , Temp: ${String.format("%.1f", batteryTemp)}°C, Voltage: ${String.format("%.1f", batteryVoltage)}V, Status: ${batteryStatus}"
        )
        out.put("gpuInfo", gpuInfo)
        out.put("threadInfo", threadInfo)
        out.put("timestamp", serviceJson.optLong("timestamp", System.currentTimeMillis()))

        // Ensure no stray single quotes break JS string literal
        return out.toString().replace("'", "\u0027")
    }
    
    /**
     * Subscribe to WhisperBus events for real-time UI updates
     */
    private fun subscribeToWhisperEvents() {
        lifecycleScope.launch {
            WhisperBus.events.collect { event ->
                when (event) {
                    is WhisperEvent.Progress -> {
                        val progressPercent = if (event.totalMs > 0) {
                            (event.processedMs * 100 / event.totalMs).toInt()
                        } else 0
                        
                        webView.evaluateJavascript(
                            """
                            if (window.updateProgress) {
                                window.updateProgress($progressPercent, ${event.processedMs}, ${event.totalMs});
                            }
                            """.trimIndent(),
                            null
                        )
                    }
                    
                    is WhisperEvent.Log -> {
                        webView.evaluateJavascript(
                            """
                            if (window.appendLog) {
                                window.appendLog('${event.line.replace("'", "\\'")}');
                            }
                            """.trimIndent(),
                            null
                        )
                    }
                    
                    is WhisperEvent.Heartbeat -> {
                        webView.evaluateJavascript(
                            """
                            if (window.setLive) {
                                window.setLive(true);
                            }
                            """.trimIndent(),
                            null
                        )
                    }
                    
                    is WhisperEvent.Stage -> {
                        webView.evaluateJavascript(
                            """
                            if (window.setStage) {
                                window.setStage('${event.name}');
                            }
                            """.trimIndent(),
                            null
                        )
                    }
                    
                    is WhisperEvent.Rtf -> {
                        webView.evaluateJavascript(
                            """
                            if (window.setRtf) {
                                window.setRtf(${event.rtf});
                            }
                            """.trimIndent(),
                            null
                        )
                    }
                    
                    is WhisperEvent.Done -> {
                        webView.evaluateJavascript(
                            """
                            if (window.setComplete) {
                                window.setComplete(${event.ok});
                            }
                            """.trimIndent(),
                            null
                        )
                    }
                    
                    is WhisperEvent.Error -> {
                        webView.evaluateJavascript(
                            """
                            if (window.showError) {
                                window.showError('${event.message.replace("'", "\\'")}');
                            }
                            """.trimIndent(),
                            null
                        )
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        Log.e(TAG, "=== PROCESSING ACTIVITY DESTROYED ===")
        Log.e(TAG, "Stack trace:", Exception("Processing activity destroyed"))
        
        // Unregister receiver
        try {
            unregisterReceiver(connectorReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering connector receiver: ${e.message}")
        }

        // Unregister resource receiver
        resourceReceiver?.let {
            try {
                it.unregister(this)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering resource receiver: ${e.message}")
            }
        }
        Log.d(TAG, "Destroying WhisperProcessingActivity")
    }
}
