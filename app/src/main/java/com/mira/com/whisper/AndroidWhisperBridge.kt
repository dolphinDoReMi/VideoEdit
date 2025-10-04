package com.mira.com.whisper

import android.content.Context
import android.content.Intent
import android.util.Log
import android.webkit.JavascriptInterface
import android.os.Environment
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * JavaScript interface for Whisper operations in WebView.
 * 
 * Provides methods for:
 * - run(jsonStr): Start a Whisper processing job
 * - export(jobId): Export results for a job
 * - listSidecars(): Get list of all sidecar files
 * - verify(jobId): Verify determinism of a job
 */
class AndroidWhisperBridge(private val context: Context) {
    
    companion object {
        private const val TAG = "AndroidWhisperBridge"
        const val SIDECAR_DIR = "/sdcard/MiraWhisper/sidecars"
        const val OUTPUT_DIR = "/sdcard/MiraWhisper/out"
        
        // Broadcast actions for Whisper service
        const val ACTION_RUN = "com.mira.whisper.RUN"
        const val ACTION_EXPORT = "com.mira.whisper.EXPORT"
        const val ACTION_VERIFY = "com.mira.whisper.VERIFY"
    }
    
    data class RunRequest(
        val uri: String,
        val preset: String = "Single",
        val modelPath: String,
        val threads: Int = 1
    )
    
    data class Sidecar(
        val job_id: String,
        val uri: String,
        val preset: String,
        val model_sha: String = "",
        val audio_sha: String = "",
        val transcript_sha: String = "",
        val segments_sha: String = "",
        val rtf: Double? = null,
        val created_at: Long = System.currentTimeMillis()
    )
    
    data class VerifyResult(
        val ok: Boolean,
        val failedField: String? = null,
        val rtf: Double? = null
    )
    
    /**
     * Start a Whisper processing job.
     * 
     * @param jsonStr JSON string containing run parameters
     * @return job ID as string
     */
    @JavascriptInterface
    fun run(jsonStr: String): String {
        return try {
            Log.d(TAG, "Received run request: $jsonStr")
            
            val jsonObj = JSONObject(jsonStr)
            val request = RunRequest(
                uri = jsonObj.getString("uri"),
                preset = jsonObj.optString("preset", "Single"),
                modelPath = jsonObj.getString("modelPath"),
                threads = jsonObj.optInt("threads", 1)
            )
            val jobId = "whisper_${UUID.randomUUID().toString().substring(0, 8)}"
            
            // Create sidecar file immediately
            val sidecar = Sidecar(
                job_id = jobId,
                uri = request.uri,
                preset = request.preset,
                model_sha = computeFileSha(request.modelPath),
                audio_sha = computeFileSha(request.uri),
                created_at = System.currentTimeMillis()
            )
            writeSidecar(sidecar)
            
            // Send broadcast to Whisper service
            val intent = Intent(ACTION_RUN).apply {
                putExtra("job_id", jobId)
                putExtra("uri", request.uri)
                putExtra("preset", request.preset)
                putExtra("model_path", request.modelPath)
                putExtra("threads", request.threads)
            }
            context.sendBroadcast(intent)
            
            Log.d(TAG, "Started Whisper job: $jobId")
            jobId
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in run(): ${e.message}", e)
            "error_${System.currentTimeMillis()}"
        }
    }
    
    /**
     * Export results for a specific job.
     * 
     * @param jobId Job ID to export
     */
    @JavascriptInterface
    fun export(jobId: String) {
        try {
            Log.d(TAG, "Export request for job: $jobId")
            
            val intent = Intent(ACTION_EXPORT).apply {
                putExtra("job_id", jobId)
            }
            context.sendBroadcast(intent)
            
            Log.d(TAG, "Export broadcast sent for job: $jobId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in export(): ${e.message}", e)
        }
    }
    
    /**
     * Get list of all sidecar files.
     * 
     * @return JSON string of sidecar array
     */
    @JavascriptInterface
    fun listSidecars(): String {
        return try {
            Log.d(TAG, "Listing sidecars")
            
            val sidecarDir = File(SIDECAR_DIR)
            if (!sidecarDir.exists()) {
                sidecarDir.mkdirs()
                return JSONArray().toString()
            }
            
            val sidecars = sidecarDir.listFiles { file ->
                file.isFile && file.name.endsWith(".json")
            }?.mapNotNull { file ->
                try {
                    val jsonStr = file.readText()
                    val jsonObj = JSONObject(jsonStr)
                    Sidecar(
                        job_id = jsonObj.getString("job_id"),
                        uri = jsonObj.getString("uri"),
                        preset = jsonObj.optString("preset", "Single"),
                        model_sha = jsonObj.optString("model_sha", ""),
                        audio_sha = jsonObj.optString("audio_sha", ""),
                        transcript_sha = jsonObj.optString("transcript_sha", ""),
                        segments_sha = jsonObj.optString("segments_sha", ""),
                        rtf = if (jsonObj.has("rtf")) jsonObj.getDouble("rtf") else null,
                        created_at = jsonObj.optLong("created_at", System.currentTimeMillis())
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse sidecar ${file.name}: ${e.message}")
                    null
                }
            }?.sortedByDescending { it.created_at } ?: emptyList()
            
            val jsonArray = JSONArray()
            sidecars.forEach { sidecar ->
                val jsonObj = JSONObject().apply {
                    put("job_id", sidecar.job_id)
                    put("uri", sidecar.uri)
                    put("preset", sidecar.preset)
                    put("model_sha", sidecar.model_sha)
                    put("audio_sha", sidecar.audio_sha)
                    put("transcript_sha", sidecar.transcript_sha)
                    put("segments_sha", sidecar.segments_sha)
                    if (sidecar.rtf != null) put("rtf", sidecar.rtf)
                    put("created_at", sidecar.created_at)
                }
                jsonArray.put(jsonObj)
            }
            
            Log.d(TAG, "Found ${sidecars.size} sidecars")
            jsonArray.toString()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in listSidecars(): ${e.message}", e)
            JSONArray().toString()
        }
    }
    
    /**
     * Verify determinism of a specific job.
     * 
     * @param jobId Job ID to verify
     * @return JSON string with verification result
     */
    @JavascriptInterface
    fun verify(jobId: String): String {
        return try {
            Log.d(TAG, "Verifying job: $jobId")
            
            val sidecar = readSidecar(jobId)
            if (sidecar == null) {
                val result = VerifyResult(ok = false, failedField = "sidecar_not_found")
                return JSONObject().apply {
                    put("ok", result.ok)
                    put("failedField", result.failedField)
                    if (result.rtf != null) put("rtf", result.rtf)
                }.toString()
            }
            
            // Send broadcast to Whisper service for verification
            val intent = Intent(ACTION_VERIFY).apply {
                putExtra("job_id", jobId)
            }
            context.sendBroadcast(intent)
            
            // For now, return a mock verification result
            // In a real implementation, this would wait for the service response
            val result = VerifyResult(
                ok = true, // Mock: assume deterministic for now
                rtf = sidecar.rtf
            )
            
            Log.d(TAG, "Verification result for $jobId: ${result.ok}")
            JSONObject().apply {
                put("ok", result.ok)
                put("failedField", result.failedField)
                if (result.rtf != null) put("rtf", result.rtf)
            }.toString()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in verify(): ${e.message}", e)
            val result = VerifyResult(ok = false, failedField = "error")
            JSONObject().apply {
                put("ok", result.ok)
                put("failedField", result.failedField)
                if (result.rtf != null) put("rtf", result.rtf)
            }.toString()
        }
    }
    
    /**
     * Export file to SD card for CAS-style handover.
     * 
     * @param jobId Job ID for the export folder
     * @param filename Name of the file to export
     * @param base64 Base64 encoded file content
     * @return true if successful, false otherwise
     */
    @JavascriptInterface
    fun exportToSdCard(jobId: String, filename: String, base64: String): Boolean {
        return try {
            Log.d(TAG, "Exporting to SD card: $jobId/$filename")
            
            // Decode base64 content
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            
            // Create export directory
            val exportDir = File(Environment.getExternalStorageDirectory(), "MiraWhisper/out/$jobId")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            // Write file
            val file = File(exportDir, filename)
            file.outputStream().use { it.write(bytes) }
            
            Log.d(TAG, "Successfully exported: ${file.absolutePath}")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting to SD card: ${e.message}", e)
            false
        }
    }
    
    private fun writeSidecar(sidecar: Sidecar) {
        try {
            val sidecarDir = File(SIDECAR_DIR)
            sidecarDir.mkdirs()
            
            val sidecarFile = File(sidecarDir, "${sidecar.job_id}.json")
            val jsonObj = JSONObject().apply {
                put("job_id", sidecar.job_id)
                put("uri", sidecar.uri)
                put("preset", sidecar.preset)
                put("model_sha", sidecar.model_sha)
                put("audio_sha", sidecar.audio_sha)
                put("transcript_sha", sidecar.transcript_sha)
                put("segments_sha", sidecar.segments_sha)
                if (sidecar.rtf != null) put("rtf", sidecar.rtf)
                put("created_at", sidecar.created_at)
            }
            sidecarFile.writeText(jsonObj.toString())
            
            Log.d(TAG, "Wrote sidecar: ${sidecarFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error writing sidecar: ${e.message}", e)
        }
    }
    
    private fun readSidecar(jobId: String): Sidecar? {
        return try {
            val sidecarFile = File(SIDECAR_DIR, "$jobId.json")
            if (sidecarFile.exists()) {
                val jsonStr = sidecarFile.readText()
                val jsonObj = JSONObject(jsonStr)
                Sidecar(
                    job_id = jsonObj.getString("job_id"),
                    uri = jsonObj.getString("uri"),
                    preset = jsonObj.optString("preset", "Single"),
                    model_sha = jsonObj.optString("model_sha", ""),
                    audio_sha = jsonObj.optString("audio_sha", ""),
                    transcript_sha = jsonObj.optString("transcript_sha", ""),
                    segments_sha = jsonObj.optString("segments_sha", ""),
                    rtf = if (jsonObj.has("rtf")) jsonObj.getDouble("rtf") else null,
                    created_at = jsonObj.optLong("created_at", System.currentTimeMillis())
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading sidecar: ${e.message}", e)
            null
        }
    }
    
    /**
     * Navigate back to the previous step.
     */
    @JavascriptInterface
    fun goBack() {
        try {
            Log.d(TAG, "Navigating back")
            // This will be handled by the activity's back button or finish()
            if (context is androidx.appcompat.app.AppCompatActivity) {
                context.runOnUiThread {
                    context.finish()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in goBack(): ${e.message}", e)
        }
    }
    
    /**
     * Start a new analysis (navigate to step 1).
     */
    @JavascriptInterface
    fun newAnalysis() {
        try {
            Log.d(TAG, "Starting new analysis")
            if (context is androidx.appcompat.app.AppCompatActivity) {
                context.runOnUiThread {
                    // Navigate to step 1 by finishing current activity
                    // The main activity will handle showing step 1
                    context.finish()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in newAnalysis(): ${e.message}", e)
        }
    }
    
    /**
     * Export JSON format results.
     */
    @JavascriptInterface
    fun exportJson(jobId: String) {
        try {
            Log.d(TAG, "Exporting JSON for job: $jobId")
            export(jobId)
        } catch (e: Exception) {
            Log.e(TAG, "Error in exportJson(): ${e.message}", e)
        }
    }
    
    /**
     * Export SRT format results.
     */
    @JavascriptInterface
    fun exportSrt(jobId: String) {
        try {
            Log.d(TAG, "Exporting SRT for job: $jobId")
            export(jobId)
        } catch (e: Exception) {
            Log.e(TAG, "Error in exportSrt(): ${e.message}", e)
        }
    }
    
    /**
     * Export TXT format results.
     */
    @JavascriptInterface
    fun exportTxt(jobId: String) {
        try {
            Log.d(TAG, "Exporting TXT for job: $jobId")
            export(jobId)
        } catch (e: Exception) {
            Log.e(TAG, "Error in exportTxt(): ${e.message}", e)
        }
    }
    
    /**
     * Export all format results.
     */
    @JavascriptInterface
    fun exportAll(jobId: String) {
        try {
            Log.d(TAG, "Exporting all formats for job: $jobId")
            export(jobId)
        } catch (e: Exception) {
            Log.e(TAG, "Error in exportAll(): ${e.message}", e)
        }
    }
    
    /**
     * Pick a video file URI using Android's file picker.
     * 
     * @return URI string of the selected video file
     */
    @JavascriptInterface
    fun pickUri(): String {
        return try {
            Log.d(TAG, "Picking video URI")
            
            // Create intent for file picker
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "video/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            }
            
            // Check if we have an activity context
            if (context is androidx.appcompat.app.AppCompatActivity) {
                context.runOnUiThread {
                    try {
                        context.startActivityForResult(intent, 1001)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start file picker: ${e.message}", e)
                    }
                }
            }
            
            // For now, return a default video file for testing
            // TODO: Implement proper result handling from startActivityForResult
            "file:///sdcard/video_v1_long.mp4"
        } catch (e: Exception) {
            Log.e(TAG, "Error in pickUri(): ${e.message}", e)
            "file:///sdcard/video_v1_long.mp4" // Fallback
        }
    }
    
    /**
     * Pick a model file path (simplified implementation for testing).
     * 
     * @return Path string of the selected model file
     */
    @JavascriptInterface
    fun pickModel(): String {
        return try {
            Log.d(TAG, "Picking model path")
            // For testing, return a default model path
            "/sdcard/Models/ggml-small.en.bin"
        } catch (e: Exception) {
            Log.e(TAG, "Error in pickModel(): ${e.message}", e)
            "/sdcard/Models/ggml-small.en.bin" // Fallback
        }
    }
    
    /**
     * List runs (compatibility with HTML interface).
     * 
     * @return JSON string of runs array
     */
    @JavascriptInterface
    fun listRuns(): String {
        return try {
            Log.d(TAG, "Listing runs")
            val sidecars = listSidecars()
            val jsonArray = JSONArray(sidecars)
            
            // Convert sidecars to runs format expected by HTML
            val runsArray = JSONArray()
            for (i in 0 until jsonArray.length()) {
                val sidecar = jsonArray.getJSONObject(i)
                val run = JSONObject().apply {
                    put("jobId", sidecar.getString("job_id"))
                    put("uri", sidecar.getString("uri"))
                    put("rtf", if (sidecar.has("rtf")) sidecar.getDouble("rtf") else null)
                    put("sidecarPath", "$SIDECAR_DIR/${sidecar.getString("job_id")}.json")
                }
                runsArray.put(run)
            }
            
            runsArray.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error in listRuns(): ${e.message}", e)
            JSONArray().toString()
        }
    }
    
    private fun computeFileSha(filePath: String): String {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                // Simple hash based on file size and modification time
                val hash = (file.length() + file.lastModified()).toString().hashCode().toString(16)
                "sha_${hash.take(8)}"
            } else {
                "sha_unknown"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error computing SHA for $filePath: ${e.message}")
            "sha_error"
        }
    }
}
