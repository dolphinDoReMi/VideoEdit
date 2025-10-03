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
        
        // New verification actions
        const val ACTION_CLIP_RUN = "com.mira.videoeditor.debug.CLIP.RUN"
        const val ACTION_VERIFY_REPRESENTATION = "com.mira.videoeditor.debug.VERIFY_REPRESENTATION"
        const val ACTION_VERIFY_RETRIEVAL = "com.mira.videoeditor.debug.VERIFY_RETRIEVAL"
        const val ACTION_VERIFY_REPRODUCIBILITY = "com.mira.videoeditor.debug.VERIFY_REPRODUCIBILITY"
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
                            "norm" to norm,
                            // Include full 512-float embedding vector for exact reproducibility
                            "vector" to embedding.toList()
                        ))
                        
                        val file = java.io.File(context.filesDir, "ops_selftest_video.json")
                        file.writeText(result.toString())

                        // Also write to external files dir for easy adb access
                        val extDir = context.getExternalFilesDir(null)
                        if (extDir != null) {
                            val extFile = java.io.File(extDir, "ops_selftest_video.json")
                            extFile.parentFile?.mkdirs()
                            extFile.writeText(result.toString())
                            Log.i(TAG, "Wrote external JSON: ${extFile.absolutePath}")
                        }

                        // Also write to public directory for guaranteed adb access
                        runCatching {
                            val publicDir = java.io.File("/sdcard/MiraClip/out")
                            publicDir.mkdirs()
                            val publicFile = java.io.File(publicDir, "ops_selftest_video.json")
                            publicFile.writeText(result.toString())
                            Log.i(TAG, "Wrote public JSON: ${publicFile.absolutePath}")
                        }.onFailure {
                            Log.w(TAG, "Failed writing public JSON: ${it.message}")
                        }
                        
                        Log.i(TAG, "Video self-test complete: frames=${frames.size}, dim=${embedding.size}, norm=$norm")
                    }
                    
                    // New verification actions
                    ACTION_CLIP_RUN -> {
                        val test = intent.getStringExtra("test")
                        Log.i(TAG, "CLIP.RUN received with test: $test")
                        
                        val result = JSONObject(mapOf(
                            "status" to "received",
                            "test" to test,
                            "timestamp" to System.currentTimeMillis()
                        ))
                        
                        val file = java.io.File(context.filesDir, "clip_run_test.json")
                        file.writeText(result.toString())
                        
                        Log.i(TAG, "CLIP.RUN test completed")
                    }
                    
                    ACTION_VERIFY_REPRESENTATION -> {
                        val outputDir = intent.getStringExtra("output_dir") ?: "/sdcard/MiraClip/test/representation"
                        Log.i(TAG, "VERIFY_REPRESENTATION: output_dir=$outputDir")
                        
                        // Create placeholder representation verification
                        val result = JSONObject(mapOf(
                            "status" to "placeholder",
                            "message" to "Representation verification not yet implemented",
                            "output_dir" to outputDir,
                            "timestamp" to System.currentTimeMillis()
                        ))
                        
                        val file = java.io.File(outputDir, "representation_verification.json")
                        file.parentFile?.mkdirs()
                        file.writeText(result.toString())
                        
                        Log.i(TAG, "Representation verification placeholder completed")
                    }
                    
                    ACTION_VERIFY_RETRIEVAL -> {
                        val outputDir = intent.getStringExtra("output_dir") ?: "/sdcard/MiraClip/test/retrieval"
                        Log.i(TAG, "VERIFY_RETRIEVAL: output_dir=$outputDir")
                        
                        // Create placeholder retrieval verification
                        val result = JSONObject(mapOf(
                            "status" to "placeholder",
                            "message" to "Retrieval verification not yet implemented",
                            "output_dir" to outputDir,
                            "timestamp" to System.currentTimeMillis()
                        ))
                        
                        val file = java.io.File(outputDir, "retrieval_verification.json")
                        file.parentFile?.mkdirs()
                        file.writeText(result.toString())
                        
                        Log.i(TAG, "Retrieval verification placeholder completed")
                    }
                    
                    ACTION_VERIFY_REPRODUCIBILITY -> {
                        val outputDir = intent.getStringExtra("output_dir") ?: "/sdcard/MiraClip/test/reproducibility"
                        Log.i(TAG, "VERIFY_REPRODUCIBILITY: output_dir=$outputDir")
                        
                        // Create placeholder reproducibility verification
                        val result = JSONObject(mapOf(
                            "status" to "placeholder",
                            "message" to "Reproducibility verification not yet implemented",
                            "output_dir" to outputDir,
                            "timestamp" to System.currentTimeMillis()
                        ))
                        
                        val file = java.io.File(outputDir, "reproducibility_verification.json")
                        file.parentFile?.mkdirs()
                        file.writeText(result.toString())
                        
                        Log.i(TAG, "Reproducibility verification placeholder completed")
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
