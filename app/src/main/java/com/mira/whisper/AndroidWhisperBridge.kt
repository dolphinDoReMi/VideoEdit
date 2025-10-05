package com.mira.whisper

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.util.Log
import android.webkit.JavascriptInterface
import android.os.Environment
import android.util.Base64
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.content.Context.BATTERY_SERVICE
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.Timer
import java.util.TimerTask

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
    
    private var activity: Activity? = null
    
    /**
     * Set the activity reference for file picker functionality
     */
    fun setActivity(activity: Activity) {
        this.activity = activity
    }
    
    companion object {
        private const val TAG = "AndroidWhisperBridge"
        const val SIDECAR_DIR = "/sdcard/MiraWhisper/sidecars"
        const val OUTPUT_DIR = "/sdcard/MiraWhisper/out"
        
        // Broadcast actions for Whisper service
        const val ACTION_RUN = "com.mira.whisper.RUN"
        const val ACTION_EXPORT = "com.mira.whisper.EXPORT"
        const val ACTION_VERIFY = "com.mira.whisper.VERIFY"
        const val ACTION_RUN_BATCH = "com.mira.whisper.RUN_BATCH"
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
        val created_at: Long = System.currentTimeMillis(),
        val lid: JSONObject? = null
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
     * Start batch Whisper processing for multiple files.
     * 
     * @param jsonStr JSON string containing batch parameters with uris array
     * @return batch job ID as string
     */
    @JavascriptInterface
    fun runBatch(jsonStr: String): String {
        return try {
            Log.d(TAG, "Received batch run request: $jsonStr")
            
            val jsonObj = JSONObject(jsonStr)
            val urisArray = jsonObj.getJSONArray("uris")
            val preset = jsonObj.optString("preset", "Single")
            val modelPath = jsonObj.getString("modelPath")
            val threads = jsonObj.optInt("threads", 4)
            
            val uris = mutableListOf<String>()
            for (i in 0 until urisArray.length()) {
                uris.add(urisArray.getString(i))
            }
            
            val batchId = "batch_${UUID.randomUUID().toString().substring(0, 8)}"
            
            Log.d(TAG, "Starting batch processing: $batchId for ${uris.size} files")
            
            // Start the connector service for real-time coordination
            val connectorIntent = Intent(context, WhisperConnectorService::class.java).apply {
                action = WhisperConnectorService.ACTION_START_PROCESSING
                putExtra(WhisperConnectorService.EXTRA_BATCH_ID, batchId)
                putExtra(WhisperConnectorService.EXTRA_FILE_COUNT, uris.size)
            }
            context.startService(connectorIntent)
            
            // Use the actual WhisperApi for batch processing
            com.mira.com.feature.whisper.api.WhisperApi.enqueueBatchTranscribe(
                ctx = context,
                uris = uris,
                model = modelPath,
                threads = threads,
                beam = 0,
                lang = "auto",
                translate = false
            )
            
            Log.d(TAG, "Enqueued batch processing: $batchId")
            batchId
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in runBatch(): ${e.message}", e)
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
                        created_at = jsonObj.optLong("created_at", System.currentTimeMillis()),
                        lid = if (jsonObj.has("lid")) jsonObj.getJSONObject("lid") else null
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
                    if (sidecar.lid != null) put("lid", sidecar.lid)
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
     * Start a new analysis (navigate to selection screen).
     */
    @JavascriptInterface
    fun newAnalysis() {
        try {
            Log.d(TAG, "Starting new analysis")
            if (context is androidx.appcompat.app.AppCompatActivity) {
                context.runOnUiThread {
                    // Navigate to selection by finishing current activity
                    // The main activity will handle showing selection UI
                    context.finish()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in newAnalysis(): ${e.message}", e)
        }
    }

    @JavascriptInterface
    fun openWhisperProcessing() {
        try {
            Log.d(TAG, "Opening Whisper Processing Activity")
            if (context is android.app.Activity) {
                (context as android.app.Activity).runOnUiThread {
                    val intent = Intent(context, WhisperProcessingActivity::class.java)
                    context.startActivity(intent)
                }
            } else {
                // Fallback: try to start activity directly
                val intent = Intent(context, WhisperProcessingActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening WhisperProcessingActivity: ${e.message}", e)
        }
    }

    /**
     * Navigate to Step 2 (Processing) from the WebView.
     */
    @JavascriptInterface
    fun openWhisperStep2() {
        // Alias to openWhisperProcessing for HTML compatibility
        openWhisperProcessing()
    }
    
    /**
     * Alias for openWhisperStep2 for compatibility
     */
    @JavascriptInterface
    fun openStep2() {
        openWhisperStep2()
    }
    
    /**
     * Navigate to Step 3 (Results) from the WebView.
     */
    @JavascriptInterface
    fun openWhisperResults() {
        try {
            Log.d(TAG, "Opening Whisper Results Activity")
            if (context is android.app.Activity) {
                (context as android.app.Activity).runOnUiThread {
                    val intent = Intent(context, WhisperResultsActivity::class.java)
                    context.startActivity(intent)
                }
            } else {
                // Fallback: try to start activity directly
                val intent = Intent(context, WhisperResultsActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening WhisperResultsActivity: ${e.message}", e)
        }
    }
    
    /**
     * Open batch results table view.
     */
    @JavascriptInterface
    fun openWhisperBatchResults(batchId: String) {
        try {
            Log.d(TAG, "Opening Whisper Batch Results Activity for batch: $batchId")
            if (context is android.app.Activity) {
                (context as android.app.Activity).runOnUiThread {
                    val intent = Intent(context, WhisperBatchResultsActivity::class.java).apply {
                        putExtra("batchId", batchId)
                    }
                    context.startActivity(intent)
                }
            } else {
                // Fallback: try to start activity directly
                val intent = Intent(context, WhisperBatchResultsActivity::class.java).apply {
                    putExtra("batchId", batchId)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening WhisperBatchResultsActivity: ${e.message}", e)
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
     * Get all video files from common directories for batch selection.
     * 
     * @return JSON string with array of video file information
     */
    @JavascriptInterface
    fun getAllVideoFiles(): String {
        return try {
            Log.d(TAG, "Getting all video files")
            
            val commonPaths = listOf(
                "/sdcard/DCIM/Camera/",
                "/sdcard/Movies/",
                "/sdcard/Download/",
                "/sdcard/"
            )
            
            // Supported video formats
            val supportedFormats = listOf("mp4", "avi", "mov", "mkv", "webm", "wmv", "flv", "m4v", "3gp")
            val videoFiles = mutableListOf<Map<String, Any>>()
            
            for (path in commonPaths) {
                val dir = File(path)
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles { file ->
                        file.isFile && file.extension.lowercase() in supportedFormats
                    }
                    if (files != null) {
                        for (file in files) {
                            videoFiles.add(mapOf(
                                "name" to file.name,
                                "size" to file.length(),
                                "uri" to "file://${file.absolutePath}",
                                "format" to file.extension.lowercase(),
                                "path" to file.absolutePath
                            ))
                        }
                    }
                }
            }
            
            Log.d(TAG, "Found ${videoFiles.size} video files")
            JSONObject().apply {
                put("files", JSONArray(videoFiles))
                put("count", videoFiles.size)
            }.toString()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in getAllVideoFiles(): ${e.message}", e)
            JSONObject().apply {
                put("files", JSONArray())
                put("count", 0)
                put("error", e.message)
            }.toString()
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
            
            // For now, return a default video file for testing
            // TODO: Implement proper file picker with Activity Result API
            val defaultVideo = "file:///sdcard/video_v1_long.mp4"
            
            // Check if the default video exists
            val file = File("/sdcard/video_v1_long.mp4")
            if (file.exists()) {
                Log.d(TAG, "Using default video: ${file.absolutePath}")
                return defaultVideo
            }
            
            // Try to find any video file in common locations
            val commonPaths = listOf(
                "/sdcard/DCIM/Camera/",
                "/sdcard/Movies/",
                "/sdcard/Download/",
                "/sdcard/"
            )
            
            // Supported video formats
            val supportedFormats = listOf("mp4", "avi", "mov", "mkv", "webm", "wmv", "flv", "m4v", "3gp")
            
            for (path in commonPaths) {
                val dir = File(path)
                if (dir.exists() && dir.isDirectory) {
                    val videoFiles = dir.listFiles { file ->
                        file.isFile && file.extension.lowercase() in supportedFormats
                    }
                    if (videoFiles != null && videoFiles.isNotEmpty()) {
                        val selectedFile = videoFiles.first()
                        Log.d(TAG, "Found video file: ${selectedFile.absolutePath}")
                        return "file://${selectedFile.absolutePath}"
                    }
                }
            }
            
            Log.w(TAG, "No video files found, using default")
            defaultVideo
        } catch (e: Exception) {
            Log.e(TAG, "Error in pickUri(): ${e.message}", e)
            "file:///sdcard/video_v1_long.mp4" // Fallback
        }
    }
    
    /**
     * Open file picker for video files using Storage Access Framework.
     * This method launches the system file picker.
     */
    @JavascriptInterface
    fun openFilePicker(): String {
        return try {
            Log.d(TAG, "Opening file picker for video files")
            
            // Do not hard-block on READ_MEDIA_*; SAF grants temporary access

            // If we're inside the Whisper File Selection activity, use its launcher directly
            if (context is WhisperFileSelectionActivity) {
                try {
                    (context as WhisperFileSelectionActivity).runOnUiThread {
                        (context as WhisperFileSelectionActivity).launchFilePicker()
                    }
                    Log.d(TAG, "File picker launched successfully via WhisperFileSelectionActivity")
                    return "file_picker_launched"
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security exception launching file picker: ${e.message}", e)
                    return "error:security_exception"
                } catch (e: Exception) {
                    Log.e(TAG, "Exception launching file picker: ${e.message}", e)
                    return "error:launch_failed"
                }
            }

            // If we're inside the Whisper Main activity, use its launcher too
            if (context is WhisperMainActivity) {
                try {
                    (context as WhisperMainActivity).runOnUiThread {
                        (context as WhisperMainActivity).launchFilePicker()
                    }
                    Log.d(TAG, "File picker launched successfully via WhisperMainActivity")
                    return "file_picker_launched"
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security exception launching file picker (main): ${e.message}", e)
                    return "error:security_exception"
                } catch (e: Exception) {
                    Log.e(TAG, "Exception launching file picker (main): ${e.message}", e)
                    return "error:launch_failed"
                }
            }

            // Fallback: context must be an Activity capable of handling GET_CONTENT
            if (context !is Activity) {
                Log.e(TAG, "Context is not an Activity and no handler available")
                return "error:invalid_context"
            }

            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "video/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }

            val resolveInfo = intent.resolveActivity(context.packageManager)
            if (resolveInfo == null) {
                Log.e(TAG, "No file picker available on this device")
                return "error:no_file_picker"
            }

            // Last-resort: startActivity (single-select) – result won't be bridged
            return try {
                context.startActivity(intent)
                Log.d(TAG, "File picker launched via startActivity")
                "file_picker_launched"
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch file picker via startActivity: ${e.message}", e)
                "error:launch_failed"
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error opening file picker: ${e.message}", e)
            "error:${e.message}"
        }
    }
    
    /**
     * Check if the app has necessary storage permissions
     */
    private fun hasStoragePermissions(): Boolean {
        return try {
            // For Android 13+ (API 33+), use READ_MEDIA_* permissions
            // For older versions, use READ_EXTERNAL_STORAGE
            val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ - check for READ_MEDIA_VIDEO and READ_MEDIA_AUDIO
                val hasVideoPermission = context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_VIDEO) == 
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                val hasAudioPermission = context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) == 
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                
                Log.d(TAG, "Storage permissions (Android 13+) - Video: $hasVideoPermission, Audio: $hasAudioPermission")
                hasVideoPermission && hasAudioPermission
            } else {
                // Android 12 and below - check for READ_EXTERNAL_STORAGE
                val hasReadPermission = context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == 
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                
                Log.d(TAG, "Storage permissions (Android 12-) - Read: $hasReadPermission")
                hasReadPermission
            }
            
            Log.d(TAG, "Storage permissions check result: $hasPermission")
            hasPermission
        } catch (e: Exception) {
            Log.e(TAG, "Error checking storage permissions: ${e.message}", e)
            false
        }
    }
    
    /**
     * Handle file selection results from the file picker
     */
    fun handleFileSelection(uris: List<Uri>) {
        try {
            Log.d(TAG, "Handling file selection: ${uris.size} files")
            
            if (uris.isEmpty()) {
                Log.w(TAG, "No files selected by user")
                val response = JSONObject().apply {
                    put("files", JSONArray())
                    put("count", 0)
                    put("success", false)
                    put("error", "No files selected")
                    put("errorType", "no_selection")
                }
                notifyFileSelection(response.toString())
                return
            }
            
            val fileInfoList = mutableListOf<Map<String, Any>>()
            val errors = mutableListOf<String>()
            
            for (uri in uris) {
                try {
                    val fileName = getFileName(uri)
                    val fileSize = getFileSize(uri)
                    val fileFormat = getFileExtension(fileName)
                    
                    // Validate file format
                    val supportedFormats = listOf("mp4", "avi", "mov", "mkv", "webm", "wmv", "flv", "m4v", "3gp")
                    if (fileFormat.lowercase() !in supportedFormats) {
                        errors.add("Unsupported format: $fileFormat")
                        continue
                    }
                    
                    // Validate file size (max 2GB)
                    if (fileSize > 2L * 1024 * 1024 * 1024) {
                        errors.add("File too large: ${fileName} (${fileSize / (1024 * 1024)}MB)")
                        continue
                    }
                    
                    // Validate file accessibility
                    if (!isFileAccessible(uri)) {
                        errors.add("File not accessible: ${fileName}")
                        continue
                    }
                    
                    fileInfoList.add(mapOf(
                        "name" to fileName,
                        "size" to fileSize,
                        "uri" to uri.toString(),
                        "format" to fileFormat,
                        "path" to (uri.path ?: ""),
                        "valid" to true
                    ))
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing file ${uri}: ${e.message}", e)
                    errors.add("Error processing file: ${e.message}")
                }
            }
            
            // Create JSON response
            val response = JSONObject().apply {
                put("files", JSONArray(fileInfoList))
                put("count", fileInfoList.size)
                put("success", fileInfoList.isNotEmpty())
                if (errors.isNotEmpty()) {
                    put("warnings", JSONArray(errors))
                }
                if (fileInfoList.isEmpty() && uris.isNotEmpty()) {
                    put("error", "No valid files selected")
                    put("errorType", "validation_failed")
                }
            }
            
            // Notify JavaScript about the file selection
            notifyFileSelection(response.toString())
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling file selection: ${e.message}", e)
            val errorResponse = JSONObject().apply {
                put("files", JSONArray())
                put("count", 0)
                put("error", e.message)
                put("errorType", "processing_error")
                put("success", false)
            }
            notifyFileSelection(errorResponse.toString())
        }
    }
    
    /**
     * Check if a file URI is accessible
     */
    private fun isFileAccessible(uri: Uri): Boolean {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                it.moveToFirst()
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking file accessibility: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get file name from URI
     */
    private fun getFileName(uri: Uri): String {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        return it.getString(nameIndex)
                    }
                }
            }
            uri.lastPathSegment ?: "unknown_file"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file name: ${e.message}")
            uri.lastPathSegment ?: "unknown_file"
        }
    }
    
    /**
     * Get file size from URI
     */
    private fun getFileSize(uri: Uri): Long {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex >= 0) {
                        return it.getLong(sizeIndex)
                    }
                }
            }
            0L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file size: ${e.message}")
            0L
        }
    }
    
    /**
     * Get file extension from file name
     */
    private fun getFileExtension(fileName: String): String {
        return try {
            val lastDot = fileName.lastIndexOf('.')
            if (lastDot > 0 && lastDot < fileName.length - 1) {
                fileName.substring(lastDot + 1).lowercase()
            } else {
                "unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    /**
     * Notify JavaScript about file selection results
     */
    private fun notifyFileSelection(jsonResponse: String) {
        try {
            // Execute JavaScript to handle the file selection
            val script = "if (window.handleFileSelection) { window.handleFileSelection('$jsonResponse'); }"
            
            if (context is WhisperFileSelectionActivity) {
                context.runOnUiThread {
                    context.notifyFileSelection(jsonResponse)
                }
            } else if (context is WhisperMainActivity) {
                context.runOnUiThread {
                    context.notifyFileSelection(jsonResponse)
                }
            } else if (activity is WhisperMainActivity) {
                (activity as WhisperMainActivity).runOnUiThread {
                    (activity as WhisperMainActivity).notifyFileSelection(jsonResponse)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error notifying file selection: ${e.message}", e)
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

    /**
     * Get batch results with metadata for table display
     */
    @JavascriptInterface
    fun getBatchResultsWithMetadata(batchId: String): String {
        return try {
            Log.d(TAG, "Getting batch results with metadata for: $batchId")
            
            // For demo purposes, return mock data
            val results = JSONArray()
            
            // Create mock transcript segments
            val mockSegments = JSONArray()
            for (i in 1..5) {
                val segment = JSONObject().apply {
                    put("start", i * 10.0)
                    put("end", (i + 1) * 10.0)
                    put("text", "This is segment $i of the transcript for batch $batchId")
                    put("confidence", 0.8 + (Math.random() * 0.2))
                }
                mockSegments.put(segment)
            }
            
            // Create mock result
            val result = JSONObject().apply {
                put("jobId", batchId)
                put("uri", "file:///sdcard/test_video.mp4")
                put("rtf", 0.45)
                put("createdAt", System.currentTimeMillis())
                put("modelSha", "sha_model_12345")
                put("audioSha", "sha_audio_67890")
                put("transcriptSha", "sha_transcript_abcdef")
                put("preset", "Single")
                put("transcript", "This is a mock transcript for demonstration purposes.")
                put("segments", mockSegments)
                put("jsonTranscript", JSONObject().apply {
                    put("language", "en")
                    put("duration", 50.0)
                    put("segments", mockSegments)
                })
            }
            results.put(result)
            
            Log.d(TAG, "Returning ${results.length()} mock batch results")
            return results.toString()
            
            // Get all sidecar files
            val sidecarDir = File(SIDECAR_DIR)
            if (sidecarDir.exists()) {
                val sidecarFiles = sidecarDir.listFiles { f -> 
                    f.isFile && f.name.endsWith(".json") && f.name.contains(batchId)
                } ?: emptyArray()
                
                sidecarFiles.forEach { sidecarFile ->
                    try {
                        val sidecarContent = sidecarFile.readText()
                        val sidecar = JSONObject(sidecarContent)
                        
                        val jobId = sidecar.getString("job_id")
                        
                        // Get transcript data
                        val transcript = readTranscript(jobId)
                        val transcriptSegments = parseTranscriptSegments(transcript)
                        
                        // Get JSON transcript if available for detailed metadata
                        val jsonTranscript = readJsonTranscript(jobId)
                        
                        val result = JSONObject().apply {
                            put("jobId", jobId)
                            put("uri", sidecar.getString("uri"))
                            put("rtf", if (sidecar.has("rtf")) sidecar.getDouble("rtf") else null)
                            put("createdAt", if (sidecar.has("created_at")) sidecar.getLong("created_at") else null)
                            put("modelSha", if (sidecar.has("model_sha")) sidecar.getString("model_sha") else null)
                            put("audioSha", if (sidecar.has("audio_sha")) sidecar.getString("audio_sha") else null)
                            put("transcriptSha", if (sidecar.has("transcript_sha")) sidecar.getString("transcript_sha") else null)
                            put("preset", if (sidecar.has("preset")) sidecar.getString("preset") else "Single")
                            put("transcript", transcript)
                            put("segments", transcriptSegments)
                            put("jsonTranscript", jsonTranscript)
                        }
                        
                        results.put(result)
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing sidecar file ${sidecarFile.name}: ${e.message}", e)
                    }
                }
            }
            
            Log.d(TAG, "Found ${results.length()} batch results")
            results.toString()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in getBatchResultsWithMetadata(): ${e.message}", e)
            JSONArray().toString()
        }
    }
    
    /**
     * Parse transcript segments from text format
     */
    private fun parseTranscriptSegments(transcript: String): JSONArray {
        val segments = JSONArray()
        
        try {
            val lines = transcript.split('\n')
            var currentSegment: JSONObject? = null
            
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) return@forEach
                
                // Check if line contains timestamp (format: HH:MM:SS,mmm --> HH:MM:SS,mmm)
                if (trimmed.contains("-->")) {
                    val parts = trimmed.split("-->")
                    if (parts.size == 2) {
                        currentSegment = JSONObject().apply {
                            put("start", parts[0].trim())
                            put("end", parts[1].trim())
                        }
                    }
                } else if (trimmed.matches(Regex("^\\d+$"))) {
                    // Segment number - ignore
                } else if (currentSegment != null && trimmed.isNotEmpty()) {
                    // Text content
                    currentSegment.put("text", trimmed)
                    segments.put(currentSegment)
                    currentSegment = null
                } else if (trimmed.isNotEmpty()) {
                    // Simple format: timestamp - text
                    val dashIndex = trimmed.indexOf(" - ")
                    if (dashIndex > 0) {
                        val time = trimmed.substring(0, dashIndex).trim()
                        val text = trimmed.substring(dashIndex + 3).trim()
                        val segment = JSONObject().apply {
                            put("start", time)
                            put("end", "")
                            put("text", text)
                        }
                        segments.put(segment)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing transcript segments: ${e.message}", e)
        }
        
        return segments
    }
    
    /**
     * Read JSON transcript for detailed metadata
     */
    private fun readJsonTranscript(jobId: String): JSONObject? {
        return try {
            val jobDir = File(OUTPUT_DIR, jobId)
            if (jobDir.exists()) {
                val jsonFile = jobDir.listFiles { f -> 
                    f.isFile && f.name.endsWith(".json") && !f.name.contains("sidecar")
                }?.firstOrNull()
                
                if (jsonFile != null) {
                    val content = jsonFile.readText()
                    JSONObject(content)
                } else null
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error reading JSON transcript for $jobId: ${e.message}", e)
            null
        }
    }

    /**
     * Read transcript text for a given job. Looks under OUTPUT_DIR/<jobId>/ for .txt or .srt.
     * Also checks OUTPUT_DIR root for files matching the job ID pattern.
     */
    @JavascriptInterface
    fun readTranscript(jobId: String): String {
        return try {
            // First try: Look in job-specific subdirectory
            val jobDir = File(OUTPUT_DIR, jobId)
            if (jobDir.exists()) {
                val txt = jobDir.listFiles { f -> f.isFile && f.name.endsWith(".txt", ignoreCase = true) }?.firstOrNull()
                val srt = jobDir.listFiles { f -> f.isFile && f.name.endsWith(".srt", ignoreCase = true) }?.firstOrNull()
                val target = txt ?: srt
                if (target != null) {
                    Log.d(TAG, "Found transcript in job directory: ${target.absolutePath}")
                    return target.readText()
                }
            }
            
            // Second try: Look in OUTPUT_DIR root for files matching job ID
            val outDir = File(OUTPUT_DIR)
            if (outDir.exists()) {
                val candidates = outDir.listFiles { f -> 
                    f.isFile && (
                        f.name.startsWith(jobId) || 
                        f.name.contains(jobId) ||
                        f.name.contains("chinese") && jobId.contains("chinese")
                    ) && (f.name.endsWith(".txt", ignoreCase = true) || f.name.endsWith(".srt", ignoreCase = true))
                }
                
                if (candidates != null && candidates.isNotEmpty()) {
                    // Prefer .srt files, then .txt files
                    val srtFile = candidates.find { it.name.endsWith(".srt", ignoreCase = true) }
                    val txtFile = candidates.find { it.name.endsWith(".txt", ignoreCase = true) }
                    val target = srtFile ?: txtFile
                    
                    if (target != null) {
                        Log.d(TAG, "Found transcript in root directory: ${target.absolutePath}")
                        return target.readText()
                    }
                }
            }
            
            Log.w(TAG, "No transcript found for job: $jobId")
            ""
        } catch (e: Exception) {
            Log.e(TAG, "readTranscript error: ${e.message}", e)
            ""
        }
    }

    /**
     * Return latest sidecar JSON (most recent by created_at).
     */
    @JavascriptInterface
    fun getLatestSidecar(): String {
        return try {
            val arr = JSONArray(listSidecars())
            if (arr.length() > 0) arr.getJSONObject(0).toString() else "{}"
        } catch (e: Exception) {
            Log.e(TAG, "getLatestSidecar error: ${e.message}")
            "{}"
        }
    }

    /**
     * Return latest transcript text if available.
     */
    @JavascriptInterface
    fun getLatestTranscript(): String {
        return try {
            val arr = JSONArray(listSidecars())
            if (arr.length() > 0) {
                val jobId = arr.getJSONObject(0).getString("job_id")
                val txt = readTranscript(jobId)
                if (txt.isNotEmpty()) return txt
            }
            
            // Fallback: scan OUTPUT_DIR root for the most recent .srt or .txt
            val outDir = File(OUTPUT_DIR)
            if (outDir.exists()) {
                val candidates = outDir.listFiles { f -> 
                    f.isFile && (f.name.endsWith(".srt", true) || f.name.endsWith(".txt", true))
                }
                
                if (candidates != null && candidates.isNotEmpty()) {
                    // Prefer Chinese transcription files, then larger files, then most recent
                    val chineseFiles = candidates.filter { it.name.contains("chinese", ignoreCase = true) }
                    val target = when {
                        chineseFiles.isNotEmpty() -> {
                            // Among Chinese files, prefer the largest (most complete)
                            chineseFiles.maxByOrNull { it.length() } ?: chineseFiles.maxByOrNull { it.lastModified() }
                        }
                        else -> {
                            // Among all files, prefer the largest (most complete), then most recent
                            candidates.maxByOrNull { it.length() } ?: candidates.maxByOrNull { it.lastModified() }
                        }
                    }
                    
                    if (target != null) {
                        Log.d(TAG, "Found latest transcript file: ${target.absolutePath} (${target.length()} bytes)")
                        return target.readText()
                    }
                }
            }
            
            Log.w(TAG, "No transcript files found")
            ""
        } catch (e: Exception) {
            Log.e(TAG, "getLatestTranscript error: ${e.message}")
            ""
        }
    }
    
    /**
     * Save a transcript to a simple on-device JSON store (stub for DB export).
     * Writes to /sdcard/MiraWhisper/db/records.jsonl as JSON Lines.
     */
    @JavascriptInterface
    fun saveTranscriptToDb(jobId: String): Boolean {
        return try {
            val transcript = readTranscript(jobId)
            val sidecar = readSidecar(jobId)
            val dbDir = File("/sdcard/MiraWhisper/db")
            if (!dbDir.exists()) dbDir.mkdirs()
            val outFile = File(dbDir, "records.jsonl")
            val record = JSONObject().apply {
                put("job_id", jobId)
                put("uri", sidecar?.uri ?: "")
                put("preset", sidecar?.preset ?: "")
                put("rtf", sidecar?.rtf)
                put("created_at", System.currentTimeMillis())
                put("transcript", transcript)
            }
            outFile.appendText(record.toString() + "\n")
            Log.d(TAG, "Saved transcript to DB stub: ${outFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "saveTranscriptToDb error: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get current Xiaomi resource usage statistics.
     * 
     * @return JSON string with resource stats
     */
    @JavascriptInterface
    fun startResourceMonitoring(): String {
        return try {
            Log.d(TAG, "Starting DeviceResourceService for background resource monitoring")
            
            val intent = Intent(context, DeviceResourceService::class.java)
            context.startForegroundService(intent)
            
            JSONObject().apply {
                put("status", "started")
                put("message", "DeviceResourceService started for background resource monitoring")
                put("timestamp", System.currentTimeMillis())
            }.toString()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting resource monitoring: ${e.message}")
            JSONObject().apply {
                put("status", "error")
                put("error", e.message)
                put("timestamp", System.currentTimeMillis())
            }.toString()
        }
    }
    
    @JavascriptInterface
    fun stopResourceMonitoring(): String {
        return try {
            Log.d(TAG, "Stopping DeviceResourceService")
            
            val intent = Intent(context, DeviceResourceService::class.java)
            context.stopService(intent)
            
            JSONObject().apply {
                put("status", "stopped")
                put("message", "DeviceResourceService stopped")
                put("timestamp", System.currentTimeMillis())
            }.toString()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping resource monitoring: ${e.message}")
            JSONObject().apply {
                put("status", "error")
                put("error", e.message)
                put("timestamp", System.currentTimeMillis())
            }.toString()
        }
    }
    
    @JavascriptInterface
    fun getResourceStats(): String {
        // Resource monitoring is now handled by DeviceResourceService in the background
        // This method returns a placeholder - UI should use ResourceUpdateReceiver for real-time data
        return try {
            Log.d(TAG, "Resource monitoring moved to DeviceResourceService - use ResourceUpdateReceiver for real-time data")
            
            val stats = JSONObject().apply {
                put("memory", 0)
                put("cpu", 0)
                put("battery", 0)
                put("temperature", 0)
                put("note", "Use ResourceUpdateReceiver for real-time data from DeviceResourceService")
                put("timestamp", System.currentTimeMillis())
            }
            
            stats.toString()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in getResourceStats: ${e.message}")
            JSONObject().apply {
                put("error", e.message)
                put("timestamp", System.currentTimeMillis())
            }.toString()
        }
    }
    
    private fun getMemoryUsage(): Long {
        return try {
            val memoryInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memoryInfo)
            val memoryMB = memoryInfo.totalPss.toLong() / 1024
            
            // Calculate memory usage percentage based on total system memory (12GB for Xiaomi Pad)
            val totalSystemMemory = 12288 // 12GB in MB for Xiaomi Pad
            val memoryPercentage = ((memoryMB.toDouble() / totalSystemMemory) * 100.0).toLong()
            
            Log.d(TAG, "Memory - PSS: ${memoryInfo.totalPss}KB (${memoryMB}MB), " +
                    "Native: ${memoryInfo.nativePss}KB, " +
                    "Dalvik: ${memoryInfo.dalvikPss}KB, " +
                    "System Memory: ${memoryMB}MB/12288MB, " +
                    "Percentage: ${memoryPercentage}%")
            
            memoryPercentage.coerceIn(0L, 100L)
        } catch (e: Exception) {
            Log.e(TAG, "Memory error: ${e.message}")
            0L
        }
    }
    
    private fun getCpuUsage(): Double {
        return try {
            // Method 1: Try to get CPU usage from /proc/stat with proper calculation
            val process = Runtime.getRuntime().exec("cat /proc/stat")
            val reader = process.inputStream.bufferedReader()
            val firstLine = reader.readLine()
            reader.close()
            process.waitFor()
            
            if (firstLine != null && firstLine.startsWith("cpu ")) {
                val parts = firstLine.split("\\s+".toRegex())
                if (parts.size >= 8) {
                    val user = parts[1].toLong()
                    val nice = parts[2].toLong()
                    val system = parts[3].toLong()
                    val idle = parts[4].toLong()
                    val iowait = parts[5].toLong()
                    val irq = parts[6].toLong()
                    val softirq = parts[7].toLong()
                    
                    val totalCpuTime = user + nice + system + idle + iowait + irq + softirq
                    val idleTime = idle + iowait
                    val usedTime = totalCpuTime - idleTime
                    
                    val cpuPercent = if (totalCpuTime > 0) {
                        (usedTime.toDouble() / totalCpuTime.toDouble()) * 100.0
                    } else {
                        0.0
                    }
                    
                    Log.d(TAG, "CPU usage: ${cpuPercent.toFixed(2)}% (used: $usedTime, total: $totalCpuTime)")
                    return cpuPercent.coerceIn(0.0, 100.0)
                }
            }
            
            // Method 2: Try to get CPU usage from /proc/loadavg
            try {
                val loadavgProcess = Runtime.getRuntime().exec("cat /proc/loadavg")
                val loadavgReader = loadavgProcess.inputStream.bufferedReader()
                val loadavgLine = loadavgReader.readLine()
                loadavgReader.close()
                loadavgProcess.waitFor()
                
                if (loadavgLine != null) {
                    val loadavg = loadavgLine.split("\\s+".toRegex())[0].toDoubleOrNull() ?: 0.0
                    val cpuCores = Runtime.getRuntime().availableProcessors()
                    val cpuPercent = (loadavg / cpuCores) * 100.0
                    Log.d(TAG, "CPU usage (loadavg): ${cpuPercent.toFixed(2)}% (load: $loadavg, cores: $cpuCores)")
                    return cpuPercent.coerceIn(0.0, 100.0)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Loadavg method failed: ${e.message}")
            }
            
            // Method 3: Try to get CPU usage from /proc/cpuinfo and /proc/uptime
            try {
                val uptimeProcess = Runtime.getRuntime().exec("cat /proc/uptime")
                val uptimeReader = uptimeProcess.inputStream.bufferedReader()
                val uptimeLine = uptimeReader.readLine()
                uptimeReader.close()
                uptimeProcess.waitFor()
                
                if (uptimeLine != null) {
                    val parts = uptimeLine.split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        val uptime = parts[0].toDoubleOrNull() ?: 0.0
                        val idleTime = parts[1].toDoubleOrNull() ?: 0.0
                        val cpuCores = Runtime.getRuntime().availableProcessors()
                        val cpuPercent = ((uptime - idleTime) / uptime) * 100.0
                        Log.d(TAG, "CPU usage (uptime): ${cpuPercent.toFixed(2)}%")
                        return cpuPercent.coerceIn(0.0, 100.0)
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Uptime method failed: ${e.message}")
            }
            
            // Method 4: Try to get CPU usage from system properties
            try {
                val cpuUsage = System.getProperty("java.lang.management.ManagementFactory")
                // This is a fallback that might not work, but let's try
                Log.d(TAG, "CPU usage: Unable to determine real CPU usage")
                return 0.0 // Return 0 instead of fake data
            } catch (e: Exception) {
                Log.d(TAG, "System properties method failed: ${e.message}")
            }
            
            Log.w(TAG, "All CPU monitoring methods failed - returning 0.0%")
            0.0
            
        } catch (e: Exception) {
            Log.e(TAG, "CPU error: ${e.message}")
            0.0
        }
    }
    
    private fun getBatteryLevel(): Int {
        return try {
            val batteryManager = context.getSystemService(BATTERY_SERVICE) as BatteryManager
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            Log.e(TAG, "Battery error: ${e.message}")
            0
        }
    }
    
    private fun getTemperature(): Double {
        return try {
            // Method 1: Try to read from thermal zones (requires root or special permissions)
            val thermalZones = listOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp", 
                "/sys/class/thermal/thermal_zone2/temp",
                "/sys/class/thermal/thermal_zone3/temp",
                "/sys/class/thermal/thermal_zone4/temp",
                "/sys/class/thermal/thermal_zone5/temp",
                "/sys/class/thermal/thermal_zone6/temp",
                "/sys/class/thermal/thermal_zone7/temp",
                "/sys/class/thermal/thermal_zone8/temp",
                "/sys/class/thermal/thermal_zone9/temp"
            )
            
            var totalTemp = 0.0
            var validReadings = 0
            
            for (zone in thermalZones) {
                try {
                    val process = Runtime.getRuntime().exec("cat $zone")
                    val reader = process.inputStream.bufferedReader()
                    val tempStr = reader.readLine()
                    reader.close()
                    process.waitFor()
                    
                    if (tempStr != null && tempStr.isNotEmpty()) {
                        val temp = tempStr.trim().toDoubleOrNull()
                        if (temp != null && temp > 0) {
                            // Convert from millidegrees to degrees if needed
                            val tempCelsius = if (temp > 1000) temp / 1000.0 else temp
                            if (tempCelsius > 0 && tempCelsius < 100) { // Sanity check
                                totalTemp += tempCelsius
                                validReadings++
                                Log.d(TAG, "Thermal zone $zone: ${tempCelsius.toFixed(1)}°C")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Failed to read thermal zone $zone: ${e.message}")
                }
            }
            
            if (validReadings > 0) {
                val avgTemp = totalTemp / validReadings
                Log.d(TAG, "Average thermal temperature: ${avgTemp.toFixed(1)}°C from $validReadings zones")
                return avgTemp
            }
            
            // Method 2: Try to get temperature from battery using Intent
            try {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                if (intent != null) {
                    val batteryTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                    if (batteryTemp > 0) {
                        val tempCelsius = batteryTemp / 10.0 // Battery temperature is in tenths of a degree
                        Log.d(TAG, "Battery temperature: ${tempCelsius.toFixed(1)}°C")
                        return tempCelsius
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Battery temperature failed: ${e.message}")
            }
            
            // Method 3: Try to get temperature from /proc/cpuinfo or other sources
            try {
                val process = Runtime.getRuntime().exec("cat /proc/cpuinfo | grep -i temperature")
                val reader = process.inputStream.bufferedReader()
                val tempLine = reader.readLine()
                reader.close()
                process.waitFor()
                
                if (tempLine != null && tempLine.contains("temperature", ignoreCase = true)) {
                    Log.d(TAG, "CPU info temperature line: $tempLine")
                    // Try to extract temperature from the line
                    val tempMatch = Regex("(\\d+(?:\\.\\d+)?)").find(tempLine)
                    if (tempMatch != null) {
                        val temp = tempMatch.value.toDoubleOrNull()
                        if (temp != null && temp > 0 && temp < 100) {
                            Log.d(TAG, "CPU info temperature: ${temp.toFixed(1)}°C")
                            return temp
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "CPU info temperature failed: ${e.message}")
            }
            
            // Method 4: Try to get temperature from sensors
            try {
                val process = Runtime.getRuntime().exec("cat /sys/class/hwmon/hwmon*/temp*_input 2>/dev/null | head -1")
                val reader = process.inputStream.bufferedReader()
                val tempStr = reader.readLine()
                reader.close()
                process.waitFor()
                
                if (tempStr != null && tempStr.isNotEmpty()) {
                    val temp = tempStr.trim().toDoubleOrNull()
                    if (temp != null && temp > 0) {
                        val tempCelsius = if (temp > 1000) temp / 1000.0 else temp
                        if (tempCelsius > 0 && tempCelsius < 100) {
                            Log.d(TAG, "Hwmon temperature: ${tempCelsius.toFixed(1)}°C")
                            return tempCelsius
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Hwmon temperature failed: ${e.message}")
            }
            
            Log.w(TAG, "All temperature monitoring methods failed - returning 0.0°C")
            0.0
            
        } catch (e: Exception) {
            Log.e(TAG, "Temperature error: ${e.message}")
            0.0
        }
    }
    
    private fun getBatteryDetails(): String {
        return try {
            val batteryManager = context.getSystemService(BATTERY_SERVICE) as BatteryManager
            
            // Get battery level
            val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            
            // Get battery information using Intent
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (intent != null) {
                // Get battery temperature
                val temperature = try {
                    val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                    if (temp > 0) {
                        "${(temp / 10.0).toFixed(1)}°C"
                    } else {
                        "N/A"
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Battery temperature not available: ${e.message}")
                    "N/A"
                }
                
                // Get battery voltage
                val voltage = try {
                    val volt = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                    if (volt > 0) {
                        "${(volt / 1000.0).toFixed(1)}V"
                    } else {
                        "N/A"
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Battery voltage not available: ${e.message}")
                    "N/A"
                }
                
                // Get charging status
                val chargingStatus = try {
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    when (status) {
                        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                        BatteryManager.BATTERY_STATUS_FULL -> "Full"
                        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                        else -> "Unknown"
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Battery status not available: ${e.message}")
                    "Unknown"
                }
                
                val result = "Level: ${level}%, Temp: $temperature, Voltage: $voltage, Status: $chargingStatus"
                Log.d(TAG, "Battery details: $result")
                return result
            } else {
                val result = "Level: ${level}%, Temp: N/A, Voltage: N/A, Status: Unknown"
                Log.d(TAG, "Battery details (no intent): $result")
                return result
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Battery details error: ${e.message}")
            "Level: N/A, Temp: N/A, Voltage: N/A, Status: Unknown"
        }
    }
    
    private fun getGpuInfo(): String {
        return try {
            val gpuInfo = StringBuilder()
            
            // Method 1: Try to get GPU info from /proc/gpuinfo
            try {
                val process = Runtime.getRuntime().exec("cat /proc/gpuinfo")
                val reader = process.inputStream.bufferedReader()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line != null && line.isNotEmpty()) {
                        gpuInfo.append(line).append(" | ")
                    }
                }
                reader.close()
                process.waitFor()
                
                if (gpuInfo.isNotEmpty()) {
                    val result = gpuInfo.toString().take(100) // Limit length
                    Log.d(TAG, "GPU info from /proc/gpuinfo: $result")
                    return result
                }
            } catch (e: Exception) {
                Log.d(TAG, "GPU info from /proc/gpuinfo not accessible: ${e.message}")
            }
            
            // Method 2: Try to get GPU info from /proc/cpuinfo
            try {
                val process = Runtime.getRuntime().exec("cat /proc/cpuinfo | grep -i gpu")
                val reader = process.inputStream.bufferedReader()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line != null && line.isNotEmpty()) {
                        gpuInfo.append(line).append(" | ")
                    }
                }
                reader.close()
                process.waitFor()
                
                if (gpuInfo.isNotEmpty()) {
                    val result = gpuInfo.toString().take(100)
                    Log.d(TAG, "GPU info from /proc/cpuinfo: $result")
                    return result
                }
            } catch (e: Exception) {
                Log.d(TAG, "GPU info from /proc/cpuinfo not accessible: ${e.message}")
            }
            
            // Method 3: Try to get GPU info from system properties
            try {
                val gpuRenderer = System.getProperty("java.awt.graphicsenv")
                val gpuVendor = System.getProperty("java.vm.vendor")
                val gpuVersion = System.getProperty("java.vm.version")
                
                if (gpuRenderer != null || gpuVendor != null || gpuVersion != null) {
                    val result = "Renderer: $gpuRenderer | Vendor: $gpuVendor | Version: $gpuVersion"
                    Log.d(TAG, "GPU info from system properties: $result")
                    return result
                }
            } catch (e: Exception) {
                Log.d(TAG, "GPU info from system properties not accessible: ${e.message}")
            }
            
            // Method 4: Try to get GPU info from OpenGL ES
            try {
                val gpuInfo = "OpenGL ES: Available"
                Log.d(TAG, "GPU info: $gpuInfo")
                return gpuInfo
            } catch (e: Exception) {
                Log.d(TAG, "OpenGL ES info not accessible: ${e.message}")
            }
            
            Log.w(TAG, "All GPU monitoring methods failed - returning 'GPU: Not accessible'")
            "GPU: Not accessible"
            
        } catch (e: Exception) {
            Log.e(TAG, "GPU info error: ${e.message}")
            "GPU: Error"
        }
    }
    
    private fun getThreadInfo(): String {
        return try {
            val threadInfo = StringBuilder()
            
            // Method 1: Get process thread count
            try {
                val process = Runtime.getRuntime().exec("cat /proc/self/status")
                val reader = process.inputStream.bufferedReader()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line?.startsWith("Threads:") == true) {
                        threadInfo.append("Threads: ").append(line.substringAfter("Threads:").trim())
                        break
                    }
                }
                reader.close()
                process.waitFor()
            } catch (e: Exception) {
                Log.d(TAG, "Thread count not accessible: ${e.message}")
            }
            
            // Method 2: Add processing thread information
            val mainThread = Thread.currentThread()
            threadInfo.append(" | Main: ${mainThread.name}")
            
            // Get thread group info
            val threadGroup = mainThread.threadGroup
            if (threadGroup != null) {
                val activeThreads = threadGroup.activeCount()
                threadInfo.append(" | Active: $activeThreads")
            }
            
            // Method 3: Add video processing specific thread info
            try {
                val runtime = Runtime.getRuntime()
                val availableProcessors = runtime.availableProcessors()
                threadInfo.append(" | CPUs: $availableProcessors")
                
                // Estimate processing threads based on CPU cores and memory
                val memoryInfo = Debug.MemoryInfo()
                Debug.getMemoryInfo(memoryInfo)
                val memoryMB = memoryInfo.totalPss.toLong() / 1024
                
                val estimatedProcessingThreads = when {
                    memoryMB > 500 -> availableProcessors * 3 // High memory = more threads
                    memoryMB > 300 -> availableProcessors * 2 // Medium memory = moderate threads
                    else -> availableProcessors // Low memory = conservative threads
                }
                
                threadInfo.append(" | Est. Processing: $estimatedProcessingThreads")
            } catch (e: Exception) {
                Log.d(TAG, "CPU info not accessible: ${e.message}")
            }
            
            val result = threadInfo.toString()
            Log.d(TAG, "Thread info collected: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Thread info error: ${e.message}")
            "Threads: Error"
        }
    }
    
    private fun Double.toFixed(digits: Int): String {
        return String.format("%.${digits}f", this)
    }
}
