package com.mira.whisper

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.mira.whisper.service.FixedDirectWhisperService

/**
 * Fixed Whisper Service Activity
 * 
 * Activity to manage the FixedDirectWhisperService and test
 * the improved idle handling and job recovery mechanisms.
 */
class FixedWhisperServiceActivity : Activity() {
    
    companion object {
        private const val TAG = "FixedWhisperServiceActivity"
    }
    
    private lateinit var statusText: TextView
    private lateinit var startServiceBtn: Button
    private lateinit var processTennisBtn: Button
    private lateinit var recoverJobsBtn: Button
    private lateinit var checkResultsBtn: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "🔧 FixedWhisperServiceActivity created")
        
        setupUI()
    }
    
    private fun setupUI() {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        statusText = TextView(this).apply {
            text = "Fixed Whisper Service Ready"
            textSize = 16f
            setPadding(0, 0, 0, 32)
        }
        
        startServiceBtn = Button(this).apply {
            text = "Start Fixed Service"
            setOnClickListener { startFixedService() }
        }
        
        processTennisBtn = Button(this).apply {
            text = "Process tennis_interview_clip_002.mp4"
            setOnClickListener { processTennisFile() }
        }
        
        recoverJobsBtn = Button(this).apply {
            text = "Recover Stuck Jobs"
            setOnClickListener { recoverStuckJobs() }
        }
        
        checkResultsBtn = Button(this).apply {
            text = "Check Results"
            setOnClickListener { checkResults() }
        }
        
        layout.addView(statusText)
        layout.addView(startServiceBtn)
        layout.addView(processTennisBtn)
        layout.addView(recoverJobsBtn)
        layout.addView(checkResultsBtn)
        
        setContentView(layout)
    }
    
    private fun startFixedService() {
        Log.d(TAG, "🔧 Starting FixedDirectWhisperService...")
        
        try {
            val intent = Intent(this, FixedDirectWhisperService::class.java)
            startService(intent)
            
            Toast.makeText(this, "Fixed service started!", Toast.LENGTH_SHORT).show()
            updateStatus("Fixed service started")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error starting service: ${e.message}", e)
            Toast.makeText(this, "Error starting service: ${e.message}", Toast.LENGTH_SHORT).show()
            updateStatus("Error starting service: ${e.message}")
        }
    }
    
    private fun processTennisFile() {
        Log.d(TAG, "🎾 Processing tennis_interview_clip_002.mp4 with fixed service...")
        
        try {
            val intent = Intent(this, FixedDirectWhisperService::class.java).apply {
                action = FixedDirectWhisperService.ACTION_PROCESS_DIRECT
                putExtra(FixedDirectWhisperService.EXTRA_FILE_PATH, "/storage/emulated/0/MiraWhisper/in/tennis_interview_clip_002.mp4")
                putExtra(FixedDirectWhisperService.EXTRA_MODEL, "whisper-tiny.en-q5_1")
                putExtra(FixedDirectWhisperService.EXTRA_LANGUAGE, "en")
            }
            
            startService(intent)
            
            Toast.makeText(this, "Processing started with fixed service!", Toast.LENGTH_SHORT).show()
            updateStatus("Processing started with fixed service")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error processing file: ${e.message}", e)
            Toast.makeText(this, "Error processing file: ${e.message}", Toast.LENGTH_SHORT).show()
            updateStatus("Error processing file: ${e.message}")
        }
    }
    
    private fun recoverStuckJobs() {
        Log.d(TAG, "🔧 Recovering stuck jobs...")
        
        try {
            val intent = Intent(this, FixedDirectWhisperService::class.java).apply {
                action = FixedDirectWhisperService.ACTION_RECOVER_JOBS
            }
            
            startService(intent)
            
            Toast.makeText(this, "Job recovery started!", Toast.LENGTH_SHORT).show()
            updateStatus("Job recovery started")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error recovering jobs: ${e.message}", e)
            Toast.makeText(this, "Error recovering jobs: ${e.message}", Toast.LENGTH_SHORT).show()
            updateStatus("Error recovering jobs: ${e.message}")
        }
    }
    
    private fun checkResults() {
        Log.d(TAG, "🔍 Checking fixed service results...")
        
        try {
            val outputDir = java.io.File("/storage/emulated/0/MiraWhisper/out")
            val resultFiles = outputDir.listFiles()?.filter { 
                it.name.contains("tennis_interview_clip_002") && it.name.contains("fixed_real")
            } ?: emptyList()
            
            if (resultFiles.isNotEmpty()) {
                val srtFile = resultFiles.find { it.name.endsWith(".srt") }
                if (srtFile != null) {
                    val transcription = srtFile.readText()
                    Log.d(TAG, "✅ Found fixed service transcription:")
                    Log.d(TAG, transcription)
                    
                    Toast.makeText(this, "Fixed transcription found!", Toast.LENGTH_LONG).show()
                    updateStatus("Fixed transcription found: ${srtFile.name}")
                } else {
                    Toast.makeText(this, "No SRT file found", Toast.LENGTH_SHORT).show()
                    updateStatus("No SRT file found")
                }
            } else {
                Toast.makeText(this, "No fixed results found", Toast.LENGTH_SHORT).show()
                updateStatus("No fixed results found")
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
}
