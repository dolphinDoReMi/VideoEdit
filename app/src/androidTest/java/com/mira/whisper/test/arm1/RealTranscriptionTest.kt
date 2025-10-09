package com.mira.whisper.test.arm1

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mira.whisper.AndroidWhisperBridge
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RealTranscriptionTest {
    
    companion object {
        private const val TAG = "RealTranscriptionTest"
    }
    
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val filesDir = context.filesDir
    private val whisperBridge = AndroidWhisperBridge(context)
    
    @Test
    fun testRealTranscription() {
        Log.i(TAG, "🎯 Starting Real ARM-1 Transcription Test")
        
        try {
            // Get test video file from scoped storage
            val testVideoFile = File(filesDir, "in/clip_002.mp4")
            assert(testVideoFile.exists()) { "Test video file not found: ${testVideoFile.absolutePath}" }
            
            Log.i(TAG, "📹 Test video: ${testVideoFile.absolutePath} (${testVideoFile.length()} bytes)")
            
            // Create URIs for scoped storage access
            val videoUri = Uri.fromFile(testVideoFile)
            val modelUri = Uri.fromFile(File(filesDir, "models/whisper-small-q5_1.gguf"))
            
            Log.i(TAG, "🔗 Video URI: $videoUri")
            Log.i(TAG, "🔗 Model URI: $modelUri")
            
            // Measure transcription performance
            val startTime = System.currentTimeMillis()
            val startMemory = android.os.Debug.getPss()
            
            // Create transcription job configuration
            val jobConfig = JSONObject().apply {
                put("job_id", "real_arm1_test_${System.currentTimeMillis()}")
                put("file", testVideoFile.absolutePath)
                put("file_uri", videoUri.toString())
                put("model", "whisper-small-q5_1.gguf")
                put("model_uri", modelUri.toString())
                put("threads", 4)
                put("ngl", 8)
                put("strategy", "greedy")
                put("temperature", 0.0)
                put("language", "en")
                put("audio_context", 1024)
                put("chunk_hop_sec", 1.5)
                put("scoped_storage", true)
                put("real_transcription", true)
            }
            
            Log.i(TAG, "⚙️ Job configuration: ${jobConfig.toString(2)}")
            
            // Execute transcription using AndroidWhisperBridge
            Log.i(TAG, "🔄 Starting real transcription...")
            val transcriptionResult = whisperBridge.run(jobConfig.toString())
            val transcriptionJson = JSONObject(transcriptionResult)
            
            val endTime = System.currentTimeMillis()
            val endMemory = android.os.Debug.getPss()
            
            // Calculate metrics
            val wallTimeMs = endTime - startTime
            val memoryDeltaMB = (endMemory - startMemory) / 1024
            
            Log.i(TAG, "⏱️ Wall time: ${wallTimeMs}ms")
            Log.i(TAG, "💾 Memory delta: ${memoryDeltaMB}MB")
            
            // Validate transcription result
            assert(transcriptionJson.has("job_id")) { "Missing job_id in result" }
            assert(transcriptionJson.has("status")) { "Missing status in result" }
            
            val status = transcriptionJson.getString("status")
            val jobId = transcriptionJson.getString("job_id")
            
            Log.i(TAG, "📊 Transcription status: $status")
            Log.i(TAG, "🆔 Job ID: $jobId")
            
            // Try to get the actual transcript
            if (transcriptionJson.has("transcript")) {
                val transcript = transcriptionJson.getString("transcript")
                Log.i(TAG, "📝 Real transcript: $transcript")
            } else {
                Log.i(TAG, "📝 No transcript in result, checking for text field...")
                if (transcriptionJson.has("text")) {
                    val text = transcriptionJson.getString("text")
                    Log.i(TAG, "📝 Real text: $text")
                }
            }
            
            // Create result summary
            val resultSummary = """
                === REAL ARM-1 TRANSCRIPTION RESULT ===
                
                Job ID: $jobId
                Status: $status
                Video File: ${testVideoFile.absolutePath}
                Video Size: ${testVideoFile.length()} bytes
                Model: whisper-small-q5_1.gguf
                Wall Time: ${wallTimeMs}ms
                Memory Delta: ${memoryDeltaMB}MB
                
                Configuration:
                - Threads: 4
                - NGL: 8
                - Strategy: greedy
                - Temperature: 0.0
                - Language: en
                - Audio Context: 1024
                - Chunk Hop: 1.5s
                - Scoped Storage: true
                
                Result: ${transcriptionResult}
                
                === END REAL TRANSCRIPTION RESULT ===
            """.trimIndent()
            
            Log.i(TAG, resultSummary)
            
            // Write result to scoped storage
            val resultFile = File(filesDir, "arm1/real_transcription_result.txt")
            resultFile.parentFile?.mkdirs()
            resultFile.writeText(resultSummary)
            
            Log.i(TAG, "💾 Result saved to: ${resultFile.absolutePath}")
            Log.i(TAG, "✅ Real ARM-1 transcription test completed successfully!")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Real transcription test failed", e)
            throw e
        }
    }
}
