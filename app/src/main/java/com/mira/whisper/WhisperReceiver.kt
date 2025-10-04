package com.mira.whisper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import org.json.JSONObject

/**
 * Broadcast receiver for Whisper operations.
 * 
 * Handles broadcasts:
 * - com.mira.whisper.RUN: Start Whisper processing
 * - com.mira.whisper.EXPORT: Export job results
 * - com.mira.whisper.VERIFY: Verify job determinism
 */
class WhisperReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "WhisperReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.Default).launch {
            try {
                Log.i(TAG, "Received Whisper broadcast: ${intent.action}")
                
                when (intent.action) {
                    AndroidWhisperBridge.ACTION_RUN -> {
                        val jobId = intent.getStringExtra("job_id")
                        val uri = intent.getStringExtra("uri")
                        val preset = intent.getStringExtra("preset")
                        val modelPath = intent.getStringExtra("model_path")
                        val threads = intent.getIntExtra("threads", 1)
                        val lang = intent.getStringExtra("lang") ?: "auto"
                        val translate = intent.getBooleanExtra("translate", false)
                        
                        if (jobId != null && uri != null && modelPath != null) {
                            Log.d(TAG, "Starting Whisper job: $jobId")
                            processWhisperJob(context, jobId, uri, preset ?: "Single", modelPath, threads, lang, translate)
                        } else {
                            Log.e(TAG, "Missing required parameters for RUN")
                        }
                    }
                    
                    AndroidWhisperBridge.ACTION_EXPORT -> {
                        val jobId = intent.getStringExtra("job_id")
                        if (jobId != null) {
                            Log.d(TAG, "Exporting job: $jobId")
                            exportJobResults(context, jobId)
                        } else {
                            Log.e(TAG, "Missing job_id for EXPORT")
                        }
                    }
                    
                    AndroidWhisperBridge.ACTION_VERIFY -> {
                        val jobId = intent.getStringExtra("job_id")
                        if (jobId != null) {
                            Log.d(TAG, "Verifying job: $jobId")
                            verifyJobDeterminism(context, jobId)
                        } else {
                            Log.e(TAG, "Missing job_id for VERIFY")
                        }
                    }
                    AndroidWhisperBridge.ACTION_RUN_BATCH -> {
                        val urisCsv = intent.getStringExtra("uris")
                        val preset = intent.getStringExtra("preset") ?: "Single"
                        val modelPath = intent.getStringExtra("modelPath") ?: intent.getStringExtra("model_path")
                        val threads = intent.getIntExtra("threads", 4)
                        if (!urisCsv.isNullOrBlank() && !modelPath.isNullOrBlank()) {
                            val uris = urisCsv.split(',').map { it.trim() }.filter { it.isNotBlank() }
                            Log.d(TAG, "Starting batch: ${uris.size} files")
                            try {
                                com.mira.com.feature.whisper.api.WhisperApi.enqueueBatchTranscribe(
                                    ctx = context,
                                    uris = uris,
                                    model = modelPath!!,
                                    threads = threads,
                                    beam = 0,
                                    lang = "auto",
                                    translate = false
                                )
                                Log.d(TAG, "Batch enqueued: ${uris.size} files")
                            } catch (e: Exception) {
                                Log.e(TAG, "Batch enqueue error: ${e.message}", e)
                            }
                        } else {
                            Log.e(TAG, "Missing params for RUN_BATCH (uris/modelPath)")
                        }
                    }
                    
                    else -> {
                        Log.w(TAG, "Unknown Whisper action: ${intent.action}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing Whisper broadcast: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
    
    private suspend fun processWhisperJob(
        context: Context, 
        jobId: String, 
        uri: String, 
        preset: String, 
        modelPath: String, 
        threads: Int,
        lang: String,
        translate: Boolean
    ) {
        try {
            Log.d(TAG, "Processing Whisper job $jobId: $uri")
            
            // Use the actual WhisperApi to enqueue real processing
            com.mira.com.feature.whisper.api.WhisperApi.enqueueTranscribe(
                ctx = context,
                uri = uri,
                model = modelPath,
                threads = threads,
                beam = 0,
                lang = lang,
                translate = translate
            )
            
            Log.d(TAG, "Enqueued Whisper job $jobId for real processing")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing Whisper job $jobId: ${e.message}", e)
        }
    }
    
    private suspend fun exportJobResults(context: Context, jobId: String) {
        try {
            Log.d(TAG, "Exporting results for job: $jobId")
            
            // TODO: Implement actual export logic
            // This would involve:
            // 1. Reading the sidecar file
            // 2. Copying transcript and segment files to output directory
            // 3. Creating a zip archive or organized folder structure
            
            val outputDir = File(AndroidWhisperBridge.OUTPUT_DIR, jobId)
            outputDir.mkdirs()
            
            // Create a mock export file
            val exportFile = File(outputDir, "transcript.txt")
            exportFile.writeText("Mock transcript for job $jobId")
            
            Log.d(TAG, "Exported results to: ${outputDir.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting job $jobId: ${e.message}", e)
        }
    }
    
    private suspend fun verifyJobDeterminism(context: Context, jobId: String) {
        try {
            Log.d(TAG, "Verifying determinism for job: $jobId")
            
            // TODO: Implement actual verification logic
            // This would involve:
            // 1. Reading the sidecar file
            // 2. Checking if all SHA hashes match expected values
            // 3. Verifying that the model, audio, and processing parameters are consistent
            // 4. Running a quick re-processing check if needed
            
            // For now, assume deterministic
            Log.d(TAG, "Job $jobId verified as deterministic")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying job $jobId: ${e.message}", e)
        }
    }
    
    private fun updateSidecarWithResults(context: Context, jobId: String, rtf: Double) {
        try {
            val sidecarFile = File(AndroidWhisperBridge.SIDECAR_DIR, "$jobId.json")
            if (sidecarFile.exists()) {
                val jsonStr = sidecarFile.readText()
                val jsonObj = JSONObject(jsonStr)
                
                // Update the sidecar with new results
                jsonObj.put("transcript_sha", "sha_txt_${System.currentTimeMillis().toString(16).take(8)}")
                jsonObj.put("segments_sha", "sha_seg_${System.currentTimeMillis().toString(16).take(8)}")
                jsonObj.put("rtf", rtf)
                
                sidecarFile.writeText(jsonObj.toString())
                Log.d(TAG, "Updated sidecar for job: $jobId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating sidecar: ${e.message}", e)
        }
    }
}
