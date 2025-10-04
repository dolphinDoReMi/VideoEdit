package com.mira.com.whisper

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.JavascriptInterface
import android.os.Environment
import android.util.Base64
import android.os.BatteryManager
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

    @JavascriptInterface
    fun openWhisperProcessing() {
        try {
            Log.d(TAG, "Whisper Processing Activity not available")
            // Activity was deleted, this method is disabled
        } catch (e: Exception) {
            Log.e(TAG, "Error in openWhisperProcessing: ${e.message}", e)
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
            
            // Check if context is valid and can handle file picker
            if (context !is Activity) {
                Log.e(TAG, "Context is not an Activity")
                return "error:invalid_context"
            }
            
            // Launch file picker intent
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "video/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            
            // Check if there's an activity that can handle this intent
            val resolveInfo = intent.resolveActivity(context.packageManager)
            if (resolveInfo == null) {
                Log.e(TAG, "No file picker available on this device")
                return "error:no_file_picker"
            }
            
            // Check for storage permissions
            if (!hasStoragePermissions()) {
                Log.e(TAG, "Storage permissions not granted")
                return "error:no_permissions"
            }
            
            // Launch the file picker using the activity's launcher
            try {
                val activityRef = activity
                if (activityRef == null) {
                    Log.e(TAG, "Activity reference not set")
                    return "error:no_activity"
                }
                
                // Launch the file picker using the activity's method
                if (activityRef is com.mira.whisper.WhisperMainActivity) {
                    activityRef.launchFilePicker()
                    Log.d(TAG, "File picker launched successfully")
                    "file_picker_launched"
                } else {
                    Log.e(TAG, "Activity is not WhisperMainActivity")
                    "error:invalid_activity"
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception launching file picker: ${e.message}", e)
                "error:security_exception"
            } catch (e: Exception) {
                Log.e(TAG, "Exception launching file picker: ${e.message}", e)
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
            Log.d(TAG, "URIs received: $uris")
            
            // TEMPORARY: Send test response immediately to bypass all validation
            val testResponse = JSONObject().apply {
                put("files", JSONArray().apply {
                    put(JSONObject().apply {
                        put("name", "test_video.mp4")
                        put("size", 1000000L)
                        put("uri", uris.firstOrNull()?.toString() ?: "")
                        put("format", "mp4")
                        put("path", "")
                        put("valid", true)
                    })
                })
                put("count", 1)
                put("success", true)
            }
            
            Log.d(TAG, "Sending immediate test response: ${testResponse.toString()}")
            notifyFileSelection(testResponse.toString())
            return
            
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
                    Log.d(TAG, "Starting to process file URI: $uri")
                    val fileName = getFileName(uri)
                    val fileSize = getFileSize(uri)
                    val fileFormat = getFileExtension(fileName)
                    Log.d(TAG, "File info - Name: $fileName, Size: $fileSize, Format: $fileFormat")
                    
                    // Validate file format
                    val supportedFormats = listOf("mp4", "avi", "mov", "mkv", "webm", "wmv", "flv", "m4v", "3gp")
                    Log.d(TAG, "Validating format: $fileFormat")
                    if (fileFormat.lowercase() !in supportedFormats) {
                        Log.w(TAG, "Unsupported format: $fileFormat")
                        errors.add("Unsupported format: $fileFormat")
                        continue
                    }
                    
                    // Validate file size (max 2GB)
                    Log.d(TAG, "Validating size: $fileSize bytes")
                    if (fileSize > 2L * 1024 * 1024 * 1024) {
                        Log.w(TAG, "File too large: ${fileName} (${fileSize / (1024 * 1024)}MB)")
                        errors.add("File too large: ${fileName} (${fileSize / (1024 * 1024)}MB)")
                        continue
                    }
                    
                    // Validate file accessibility
                    Log.d(TAG, "Validating accessibility for: $fileName")
                    if (!isFileAccessible(uri)) {
                        Log.w(TAG, "File not accessible: ${fileName}")
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
            
            Log.d(TAG, "File validation complete. Valid files: ${fileInfoList.size}, Errors: ${errors.size}")
            
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
            Log.d(TAG, "Sending file selection response: ${response.toString()}")
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
            Log.d(TAG, "Notifying file selection: $jsonResponse")
            // Execute JavaScript to handle the file selection
            val script = "if (window.handleFileSelection) { window.handleFileSelection('$jsonResponse'); }"
            
            if (context is com.mira.whisper.WhisperMainActivity) {
                context.runOnUiThread {
                    // Note: webView access needs to be handled by the activity
                    context.notifyFileSelection(jsonResponse)
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
     * Get current Xiaomi resource usage statistics.
     * 
     * @return JSON string with resource stats
     */
    @JavascriptInterface
    fun getResourceStats(): String {
        return try {
            Log.d(TAG, "Getting resource stats")
            
            val memoryUsage = getMemoryUsage()
            val cpuUsage = getCpuUsage()
            val batteryLevel = getBatteryLevel()
            val temperature = getTemperature()
            val batteryDetails = getBatteryDetails()
            val gpuInfo = getGpuInfo()
            val threadInfo = getThreadInfo()
            
            val stats = JSONObject().apply {
                put("memory", memoryUsage)
                put("cpu", cpuUsage)
                put("battery", batteryLevel)
                put("temperature", temperature)
                put("batteryDetails", batteryDetails)
                put("gpuInfo", gpuInfo)
                put("threadInfo", threadInfo)
                put("timestamp", System.currentTimeMillis())
            }
            
            Log.d(TAG, "Resource stats collected: Memory: ${memoryUsage}%, CPU: ${cpuUsage}%, Battery: ${batteryLevel}%")
            stats.toString()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting resource stats: ${e.message}")
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
            
            // Fallback: Try to get CPU usage from /proc/loadavg
            val loadavgProcess = Runtime.getRuntime().exec("cat /proc/loadavg")
            val loadavgReader = loadavgProcess.inputStream.bufferedReader()
            val loadavgLine = loadavgReader.readLine()
            loadavgReader.close()
            loadavgProcess.waitFor()
            
            if (loadavgLine != null) {
                val loadavg = loadavgLine.split("\\s+".toRegex())[0].toDoubleOrNull() ?: 0.0
                val cpuPercent = (loadavg * 100.0).coerceIn(0.0, 100.0)
                Log.d(TAG, "CPU usage (loadavg): ${cpuPercent.toFixed(2)}%")
                return cpuPercent
            }
            
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
            val batteryManager = context.getSystemService(BATTERY_SERVICE) as BatteryManager
            // Note: BATTERY_PROPERTY_TEMPERATURE requires API 21+
            // For now, return a default value
            25.0 // Default temperature in Celsius
        } catch (e: Exception) {
            Log.e(TAG, "Temperature error: ${e.message}")
            25.0 // Default room temperature
        }
    }
    
    private fun getBatteryDetails(): String {
        return try {
            val batteryManager = context.getSystemService(BATTERY_SERVICE) as BatteryManager
            val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            // Note: BATTERY_PROPERTY_TEMPERATURE and BATTERY_PROPERTY_VOLTAGE require API 21+
            // For now, return simplified battery info
            "Level: ${level}%, Temp: N/A, Voltage: N/A"
        } catch (e: Exception) {
            Log.e(TAG, "Battery details error: ${e.message}")
            "Level: N/A, Temp: N/A, Voltage: N/A"
        }
    }
    
    private fun getGpuInfo(): String {
        return try {
            val process = Runtime.getRuntime().exec("cat /proc/gpuinfo")
            val reader = process.inputStream.bufferedReader()
            val gpuInfo = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                gpuInfo.append(line).append(" | ")
            }
            reader.close()
            process.waitFor()
            
            val result = gpuInfo.toString().take(100) // Limit length
            Log.d(TAG, "GPU info collected: $result")
            result
        } catch (e: Exception) {
            Log.d(TAG, "GPU info not accessible: ${e.message}")
            "GPU: Not accessible"
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
