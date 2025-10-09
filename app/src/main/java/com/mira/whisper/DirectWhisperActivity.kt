package com.mira.whisper

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Direct Whisper Activity
 * 
 * Activity to test direct Whisper processing using existing JNI
 * to get real transcription results from tennis_interview_clip_002.mp4.
 */
class DirectWhisperActivity : Activity() {
    
    companion object {
        private const val TAG = "DirectWhisperActivity"
    }
    
    private lateinit var statusText: TextView
    private lateinit var processBtn: Button
    private lateinit var checkResultsBtn: Button
    private lateinit var processor: DirectWhisperProcessor
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "🔧 DirectWhisperActivity created")
        
        processor = DirectWhisperProcessor(this)
        setupUI()
    }
    
    private fun setupUI() {
        // Create simple UI programmatically
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        statusText = TextView(this).apply {
            text = "Direct Whisper Processing Ready"
            textSize = 16f
            setPadding(0, 0, 0, 32)
        }
        
        processBtn = Button(this).apply {
            text = "Process tennis_interview_clip_002.mp4"
            setOnClickListener { processTennisFile() }
        }
        
        checkResultsBtn = Button(this).apply {
            text = "Check Results"
            setOnClickListener { checkResults() }
        }
        
        layout.addView(statusText)
        layout.addView(processBtn)
        layout.addView(checkResultsBtn)
        
        setContentView(layout)
    }
    
    private fun processTennisFile() {
        Log.d(TAG, "🎾 Processing tennis_interview_clip_002.mp4 with direct Whisper...")
        
        updateStatus("Processing tennis_interview_clip_002.mp4...")
        processBtn.isEnabled = false
        
        // Use coroutines for background processing
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    processor.processTennisFile()
                }
                
                when (result) {
                    is DirectWhisperProcessor.ProcessingResult.Success -> {
                        Log.d(TAG, "✅ Direct Whisper processing completed successfully")
                        Log.d(TAG, "📄 SRT file: ${result.srtFile.absolutePath}")
                        Log.d(TAG, "📄 Transcription: ${result.transcription}")
                        
                        Toast.makeText(this@DirectWhisperActivity, 
                            "Processing completed successfully!", Toast.LENGTH_LONG).show()
                        updateStatus("Processing completed successfully!")
                    }
                    
                    is DirectWhisperProcessor.ProcessingResult.Error -> {
                        Log.e(TAG, "❌ Direct Whisper processing failed: ${result.message}")
                        Toast.makeText(this@DirectWhisperActivity, 
                            "Processing failed: ${result.message}", Toast.LENGTH_LONG).show()
                        updateStatus("Processing failed: ${result.message}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in processing: ${e.message}", e)
                Toast.makeText(this@DirectWhisperActivity, 
                    "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                updateStatus("Error: ${e.message}")
            } finally {
                processBtn.isEnabled = true
            }
        }
    }
    
    private fun checkResults() {
        Log.d(TAG, "🔍 Checking direct Whisper processing results...")
        
        try {
            val outputDir = java.io.File("/storage/emulated/0/MiraWhisper/out")
            val resultFiles = outputDir.listFiles()?.filter { 
                it.name.contains("tennis_interview_clip_002") && it.name.contains("direct_real")
            } ?: emptyList()
            
            if (resultFiles.isNotEmpty()) {
                val srtFile = resultFiles.find { it.name.endsWith(".srt") }
                if (srtFile != null) {
                    val transcription = srtFile.readText()
                    Log.d(TAG, "✅ Found direct Whisper transcription:")
                    Log.d(TAG, transcription)
                    
                    Toast.makeText(this, "Direct transcription found!", Toast.LENGTH_LONG).show()
                    updateStatus("Direct transcription found: ${srtFile.name}")
                } else {
                    Toast.makeText(this, "No SRT file found", Toast.LENGTH_SHORT).show()
                    updateStatus("No SRT file found")
                }
            } else {
                Toast.makeText(this, "No direct results found", Toast.LENGTH_SHORT).show()
                updateStatus("No direct results found")
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
