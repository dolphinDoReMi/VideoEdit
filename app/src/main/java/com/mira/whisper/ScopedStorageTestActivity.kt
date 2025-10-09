package com.mira.whisper

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mira.videoeditor.infra.storage.ScopedStorageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Test activity to demonstrate scoped storage pipeline with tennis clip.
 */
class ScopedStorageTestActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "ScopedStorageTest"
    }
    
    private lateinit var scopedStorageService: ScopedStorageService
    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private lateinit var processButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "ScopedStorageTestActivity created")
        
        // Initialize scoped storage components
        scopedStorageService = ScopedStorageService(this)
        
        setupUI()
        
        // Test scoped storage pipeline
        testScopedStoragePipeline()
    }
    
    private fun setupUI() {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        statusText = TextView(this).apply {
            text = "Scoped Storage Test Ready"
            textSize = 16f
            setPadding(0, 0, 0, 16)
        }
        
        resultText = TextView(this).apply {
            text = "No results yet"
            textSize = 14f
            setPadding(0, 0, 0, 16)
        }
        
        processButton = Button(this).apply {
            text = "Process Tennis Clip with Scoped Storage"
            setOnClickListener { processTennisClip() }
        }
        
        layout.addView(statusText)
        layout.addView(resultText)
        layout.addView(processButton)
        
        setContentView(layout)
    }
    
    private fun testScopedStoragePipeline() {
        Log.d(TAG, "Testing scoped storage pipeline components")
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                updateStatus("Testing scoped storage components...")
                
                // Test 1: Check if components are available
                updateResult("✅ ScopedStorageService created")
                updateStatus("Scoped storage components ready")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error testing scoped storage pipeline", e)
                updateResult("❌ Error: ${e.message}")
                updateStatus("Scoped storage test failed")
            }
        }
    }
    
    private fun processTennisClip() {
        Log.d(TAG, "Processing tennis clip with scoped storage")
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                updateStatus("Processing tennis_interview_clip_002.mp4...")
                
                // Create URIs for the tennis clip and model
                val videoUri = Uri.parse("content://media/external/video/media/1") // This would need proper resolution
                val modelUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3AMiraWhisper%2Fmodels%2Fsmall.en-q5_1.gguf")
                
                updateResult("Video URI: $videoUri\nModel URI: $modelUri")
                
                // Test the complete pipeline
                val result = scopedStorageService.processVideoWithScopedStorage(videoUri, modelUri)
                
                if (result.success) {
                    updateResult("✅ Pipeline SUCCESS\nTranscript: ${result.transcript}")
                    updateStatus("Processing completed successfully")
                } else {
                    updateResult("❌ Pipeline FAILED: ${result.error}")
                    updateStatus("Processing failed")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing tennis clip", e)
                updateResult("❌ Error: ${e.message}")
                updateStatus("Processing failed")
            }
        }
    }
    
    private fun updateStatus(message: String) {
        runOnUiThread {
            statusText.text = message
            Log.d(TAG, "Status: $message")
        }
    }
    
    private fun updateResult(message: String) {
        runOnUiThread {
            resultText.text = message
            Log.d(TAG, "Result: $message")
        }
    }
}
