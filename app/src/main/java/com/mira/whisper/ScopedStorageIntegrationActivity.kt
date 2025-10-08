package com.mira.whisper

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.mira.whisper.service.ScopedDirectWhisperService
import com.mira.whisper.service.ScopedStorageBridgeService

/**
 * Scoped Storage Integration Activity
 * 
 * Activity to manage and test scoped storage services integration
 * with the MiraWhisper ASR system.
 */
class ScopedStorageIntegrationActivity : Activity() {
    
    companion object {
        private const val TAG = "ScopedStorageIntegration"
    }
    
    private var scopedDirectService: ScopedDirectWhisperService? = null
    private var scopedBridgeService: ScopedStorageBridgeService? = null
    private var scopedDirectBound = false
    private var scopedBridgeBound = false
    
    private lateinit var statusText: TextView
    private lateinit var startDirectServiceBtn: Button
    private lateinit var startBridgeServiceBtn: Button
    private lateinit var processFileBtn: Button
    private lateinit var checkResultsBtn: Button
    
    private val scopedDirectConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ScopedDirectWhisperService.ScopedWhisperBinder
            scopedDirectService = binder.getService()
            scopedDirectBound = true
            Log.d(TAG, "✅ ScopedDirectWhisperService connected")
            updateStatus("ScopedDirectWhisperService connected")
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            scopedDirectService = null
            scopedDirectBound = false
            Log.d(TAG, "❌ ScopedDirectWhisperService disconnected")
            updateStatus("ScopedDirectWhisperService disconnected")
        }
    }
    
    private val scopedBridgeConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ScopedStorageBridgeService.ScopedStorageBinder
            scopedBridgeService = binder.getService()
            scopedBridgeBound = true
            Log.d(TAG, "✅ ScopedStorageBridgeService connected")
            updateStatus("ScopedStorageBridgeService connected")
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            scopedBridgeService = null
            scopedBridgeBound = false
            Log.d(TAG, "❌ ScopedStorageBridgeService disconnected")
            updateStatus("ScopedStorageBridgeService disconnected")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "🔧 ScopedStorageIntegrationActivity created")
        
        setupUI()
        bindServices()
    }
    
    private fun setupUI() {
        // Create simple UI programmatically
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        statusText = TextView(this).apply {
            text = "Scoped Storage Integration Ready"
            textSize = 16f
            setPadding(0, 0, 0, 32)
        }
        
        startDirectServiceBtn = Button(this).apply {
            text = "Start Scoped Direct Service"
            setOnClickListener { startScopedDirectService() }
        }
        
        startBridgeServiceBtn = Button(this).apply {
            text = "Start Scoped Bridge Service"
            setOnClickListener { startScopedBridgeService() }
        }
        
        processFileBtn = Button(this).apply {
            text = "Process tennis_interview_clip_002.mp4"
            setOnClickListener { processTennisFile() }
        }
        
        checkResultsBtn = Button(this).apply {
            text = "Check Results"
            setOnClickListener { checkResults() }
        }
        
        layout.addView(statusText)
        layout.addView(startDirectServiceBtn)
        layout.addView(startBridgeServiceBtn)
        layout.addView(processFileBtn)
        layout.addView(checkResultsBtn)
        
        setContentView(layout)
    }
    
    private fun bindServices() {
        Log.d(TAG, "🔗 Binding scoped storage services...")
        
        // Bind to ScopedDirectWhisperService
        val directIntent = Intent(this, ScopedDirectWhisperService::class.java)
        bindService(directIntent, scopedDirectConnection, Context.BIND_AUTO_CREATE)
        
        // Bind to ScopedStorageBridgeService
        val bridgeIntent = Intent(this, ScopedStorageBridgeService::class.java)
        bindService(bridgeIntent, scopedBridgeConnection, Context.BIND_AUTO_CREATE)
    }
    
    private fun startScopedDirectService() {
        Log.d(TAG, "🚀 Starting ScopedDirectWhisperService...")
        
        val intent = Intent(this, ScopedDirectWhisperService::class.java)
        startService(intent)
        
        Toast.makeText(this, "ScopedDirectWhisperService started", Toast.LENGTH_SHORT).show()
        updateStatus("ScopedDirectWhisperService started")
    }
    
    private fun startScopedBridgeService() {
        Log.d(TAG, "🚀 Starting ScopedStorageBridgeService...")
        
        val intent = Intent(this, ScopedStorageBridgeService::class.java)
        startService(intent)
        
        Toast.makeText(this, "ScopedStorageBridgeService started", Toast.LENGTH_SHORT).show()
        updateStatus("ScopedStorageBridgeService started")
    }
    
    private fun processTennisFile() {
        Log.d(TAG, "🎾 Processing tennis_interview_clip_002.mp4...")
        
        val filePath = "/storage/emulated/0/MiraWhisper/in/tennis_interview_clip_002.mp4"
        
        if (scopedDirectBound && scopedDirectService != null) {
            scopedDirectService!!.processFile(filePath)
            Toast.makeText(this, "Processing started with ScopedDirectWhisperService", Toast.LENGTH_SHORT).show()
            updateStatus("Processing tennis_interview_clip_002.mp4...")
        } else if (scopedBridgeBound && scopedBridgeService != null) {
            scopedBridgeService!!.processFile(filePath)
            Toast.makeText(this, "Processing started with ScopedStorageBridgeService", Toast.LENGTH_SHORT).show()
            updateStatus("Processing tennis_interview_clip_002.mp4...")
        } else {
            Toast.makeText(this, "No scoped storage service available", Toast.LENGTH_SHORT).show()
            updateStatus("No scoped storage service available")
        }
    }
    
    private fun checkResults() {
        Log.d(TAG, "🔍 Checking scoped storage results...")
        
        try {
            val outputDir = java.io.File("/storage/emulated/0/MiraWhisper/out")
            if (outputDir.exists()) {
                val resultFiles = outputDir.listFiles()?.filter { 
                    it.name.contains("tennis_interview_clip_002") && 
                    (it.name.contains("scoped") || it.name.contains("bridge"))
                } ?: emptyList()
                
                if (resultFiles.isNotEmpty()) {
                    val srtFile = resultFiles.find { it.name.endsWith(".srt") }
                    if (srtFile != null) {
                        val transcription = srtFile.readText()
                        Log.d(TAG, "✅ Found scoped storage transcription:")
                        Log.d(TAG, transcription)
                        
                        Toast.makeText(this, "Scoped storage transcription found!", Toast.LENGTH_LONG).show()
                        updateStatus("Scoped storage transcription found: ${srtFile.name}")
                    } else {
                        Toast.makeText(this, "No SRT file found", Toast.LENGTH_SHORT).show()
                        updateStatus("No SRT file found")
                    }
                } else {
                    Toast.makeText(this, "No scoped storage results found", Toast.LENGTH_SHORT).show()
                    updateStatus("No scoped storage results found")
                }
            } else {
                Toast.makeText(this, "Output directory does not exist", Toast.LENGTH_SHORT).show()
                updateStatus("Output directory does not exist")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking results: ${e.message}", e)
            Toast.makeText(this, "Error checking results: ${e.message}", Toast.LENGTH_SHORT).show()
            updateStatus("Error checking results: ${e.message}")
        }
    }
    
    private fun updateStatus(message: String) {
        runOnUiThread {
            statusText.text = message
            Log.d(TAG, "📊 Status: $message")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        if (scopedDirectBound) {
            unbindService(scopedDirectConnection)
        }
        
        if (scopedBridgeBound) {
            unbindService(scopedBridgeConnection)
        }
        
        Log.d(TAG, "🛑 ScopedStorageIntegrationActivity destroyed")
    }
}
