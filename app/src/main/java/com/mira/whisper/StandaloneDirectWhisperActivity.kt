package com.mira.whisper

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class StandaloneDirectWhisperActivity : Activity() {
    
    companion object {
        private const val TAG = "StandaloneDirectWhisper"
        
        init {
            System.loadLibrary("whisper_jni")
        }
    }
    
    private lateinit var statusText: TextView
    private lateinit var processBtn: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "🔧 StandaloneDirectWhisperActivity created")
        
        setupUI()
    }
    
    private fun setupUI() {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        statusText = TextView(this).apply {
            text = "Standalone Direct Whisper Processing Ready"
            textSize = 16f
            setPadding(0, 0, 0, 32)
        }
        
        processBtn = Button(this).apply {
            text = "Process tennis_interview_clip_002.mp4"
            setOnClickListener { processTennisFile() }
        }
        
        layout.addView(statusText)
        layout.addView(processBtn)
        
        setContentView(layout)
    }
    
    private fun processTennisFile() {
        Log.d(TAG, "🎾 Processing tennis_interview_clip_002.mp4 with standalone Whisper...")
        
        try {
            val result = processTennisFileStandalone()
            Log.d(TAG, "📊 Processing result: $result")
            
            Toast.makeText(this, "Processing completed: $result", Toast.LENGTH_LONG).show()
            updateStatus("Processing completed: $result")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in processing: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            updateStatus("Error: ${e.message}")
        }
    }
    
    private fun updateStatus(message: String) {
        runOnUiThread {
            statusText.text = message
            Log.d(TAG, "📊 Status: $message")
        }
    }
    
    // JNI function declaration
    private external fun processTennisFileStandalone(): String
}
