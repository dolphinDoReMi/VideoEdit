package com.mira.clip.ops

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mira.clip.clip.ClipEngines
import com.mira.clip.services.RetrievalService
import com.mira.clip.services.VideoIngestService
import com.mira.clip.video.FrameSampler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.sqrt

/**
 * Debug Broadcast Receiver for mira_clip CLIP4Clip service.
 * 
 * Handles debug-specific broadcast actions for development and testing.
 * Includes self-test hooks for validating representation learning.
 */
class TestReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "TestReceiver"
        
        // Debug broadcast actions
        const val ACTION_INGEST_MANIFEST = "com.mira.clip.INGEST_MANIFEST"
        const val ACTION_SEARCH_MANIFEST = "com.mira.clip.SEARCH_MANIFEST"
        const val ACTION_DEBUG_LOG = "com.mira.clip.DEBUG_LOG"
        const val ACTION_CLEAR_CACHE = "com.mira.clip.CLEAR_CACHE"
        const val ACTION_SELFTEST_TEXT = "com.mira.clip.SELFTEST_TEXT"
        const val ACTION_SELFTEST_VIDEO = "com.mira.clip.SELFTEST_VIDEO"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.Default).launch {
            try {
                Log.i(TAG, "Received broadcast: ${intent.action}")
                
                when (intent.action) {
                    ACTION_INGEST_MANIFEST -> {
                        val manifestPath = intent.getStringExtra("manifest_path")
                        if (manifestPath != null) {
                            Orchestrator.ingest(context, manifestPath)
                        } else {
                            Log.e(TAG, "No manifest_path provided for INGEST_MANIFEST")
                        }
                    }
                    
                    ACTION_SEARCH_MANIFEST -> {
                        val manifestPath = intent.getStringExtra("manifest_path")
                        if (manifestPath != null) {
                            Orchestrator.search(context, manifestPath)
                        } else {
                            Log.e(TAG, "No manifest_path provided for SEARCH_MANIFEST")
                        }
                    }
                    
                    ACTION_DEBUG_LOG -> {
                        Orchestrator.debugLog(context)
                    }
                    
                    ACTION_CLEAR_CACHE -> {
                        Orchestrator.clearCache(context)
                    }
                    
                    ACTION_SELFTEST_TEXT -> {
                        val text = intent.getStringExtra("text") ?: "a photo of a cat"
                        Log.i(TAG, "Running text encoder self-test with: $text")
                        
                        val clipEngines = ClipEngines(context)
                        clipEngines.initialize()
                        val embedding = clipEngines.encodeText(text)
                        
                        val norm = sqrt(embedding.sumOf { it.toDouble() * it.toDouble() })
                        val result = JSONObject(mapOf(
                            "text" to text,
                            "dim" to embedding.size,
                            "norm" to norm
                        ))
                        
                        val file = java.io.File(context.filesDir, "ops_selftest_text.json")
                        file.writeText(result.toString())
                        
                        Log.i(TAG, "Text self-test complete: dim=${embedding.size}, norm=$norm")
                    }
                    
                    ACTION_SELFTEST_VIDEO -> {
                        val path = intent.getStringExtra("path") ?: "/sdcard/Movies/video_v1.mp4"
                        val frameCount = intent.getIntExtra("frame_count", 32)
                        Log.i(TAG, "Running video encoder self-test: path=$path, frames=$frameCount")
                        
                        val frames = FrameSampler.sampleUniform(context, path, frameCount)
                        Log.i(TAG, "Sampled ${frames.size} frames")
                        
                        val clipEngines = ClipEngines(context)
                        clipEngines.initialize()
                        val embedding = clipEngines.encodeFrames(frames)
                        
                        val norm = sqrt(embedding.sumOf { it.toDouble() * it.toDouble() })
                        val result = JSONObject(mapOf(
                            "path" to path,
                            "frame_count" to frames.size,
                            "dim" to embedding.size,
                            "norm" to norm
                        ))
                        
                        val file = java.io.File(context.filesDir, "ops_selftest_video.json")
                        file.writeText(result.toString())
                        
                        Log.i(TAG, "Video self-test complete: frames=${frames.size}, dim=${embedding.size}, norm=$norm")
                    }
                    
                    else -> {
                        Log.w(TAG, "Unknown broadcast action: ${intent.action}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing broadcast: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
