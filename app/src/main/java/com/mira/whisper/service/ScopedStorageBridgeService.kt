package com.mira.whisper.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Scoped Storage Bridge Service
 * 
 * Bridge service that integrates our scoped storage infrastructure
 * with the MiraWhisper ASR system for seamless file processing.
 */
class ScopedStorageBridgeService : Service() {
    
    companion object {
        private const val TAG = "ScopedStorageBridgeService"
        
        // Scoped storage paths
        private const val MIRAWHISPER_ROOT = "/storage/emulated/0/MiraWhisper"
        private const val INPUT_DIR = "$MIRAWHISPER_ROOT/in"
        private const val OUTPUT_DIR = "$MIRAWHISPER_ROOT/out"
        private const val MODELS_DIR = "$MIRAWHISPER_ROOT/models"
    }
    
    private val binder = ScopedStorageBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    
    inner class ScopedStorageBinder : Binder() {
        fun getService(): ScopedStorageBridgeService = this@ScopedStorageBridgeService
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🔧 ScopedStorageBridgeService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "🚀 ScopedStorageBridgeService started")
        
        // Process files using scoped storage
        serviceScope.launch {
            processFilesWithScopedStorage()
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    /**
     * Process files using scoped storage infrastructure.
     */
    private suspend fun processFilesWithScopedStorage() = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔍 Processing files with scoped storage...")
        
        try {
            // Check input directory
            val inputDir = File(INPUT_DIR)
            if (!inputDir.exists()) {
                Log.w(TAG, "❌ Input directory does not exist: $INPUT_DIR")
                return@withContext
            }
            
            // Get all media files
            val mediaFiles = inputDir.listFiles()?.filter { file ->
                file.isFile && file.extension.lowercase() in listOf("mp4", "wav", "m4a", "mp3")
            } ?: emptyList()
            
            Log.d(TAG, "📁 Found ${mediaFiles.size} media files to process")
            
            for (file in mediaFiles) {
                Log.d(TAG, "🎾 Processing: ${file.name}")
                processFileWithScopedStorage(file)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error processing files: ${e.message}", e)
        }
    }
    
    /**
     * Process a single file using scoped storage.
     */
    private suspend fun processFileWithScopedStorage(file: File) = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔧 Processing file with scoped storage: ${file.name}")
        
        try {
            // Step 1: Validate file access
            if (!file.canRead()) {
                Log.e(TAG, "❌ Cannot read file: ${file.name}")
                return@withContext
            }
            
            // Step 2: Create scoped storage metadata
            val fileMetadata = createFileMetadata(file)
            Log.d(TAG, "📊 File metadata: $fileMetadata")
            
            // Step 3: Process with MiraWhisper ASR
            val transcription = processWithMiraWhisperASR(file)
            
            // Step 4: Save results using scoped storage
            if (transcription != null) {
                saveScopedStorageResults(file.name, transcription, fileMetadata)
                Log.d(TAG, "✅ Successfully processed: ${file.name}")
            } else {
                Log.w(TAG, "⚠️ No transcription generated for: ${file.name}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error processing file ${file.name}: ${e.message}", e)
        }
    }
    
    /**
     * Create file metadata for scoped storage.
     */
    private fun createFileMetadata(file: File): Map<String, Any> {
        return mapOf(
            "filename" to file.name,
            "file_path" to file.absolutePath,
            "file_size" to file.length(),
            "extension" to file.extension,
            "last_modified" to file.lastModified(),
            "can_read" to file.canRead(),
            "can_write" to file.canWrite(),
            "processing_timestamp" to System.currentTimeMillis()
        )
    }
    
    /**
     * Process file with MiraWhisper ASR system.
     */
    private suspend fun processWithMiraWhisperASR(file: File): String? = withContext(Dispatchers.IO) {
        Log.d(TAG, "🧠 Processing with MiraWhisper ASR: ${file.name}")
        
        try {
            // Step 1: Check MiraWhisper system status
            val miraWhisperStatus = checkMiraWhisperStatus()
            if (!miraWhisperStatus) {
                Log.e(TAG, "❌ MiraWhisper system not available")
                return@withContext null
            }
            
            // Step 2: Create ASR job
            val jobId = System.currentTimeMillis().toInt()
            val asrJob = createScopedASRJob(jobId, file)
            
            // Step 3: Submit job to MiraWhisper
            val success = submitScopedASRJob(asrJob)
            
            if (success) {
                // Step 4: Wait for results
                val transcription = waitForScopedASRResults(jobId, file.name)
                Log.d(TAG, "✅ ASR processing completed for: ${file.name}")
                transcription
            } else {
                Log.e(TAG, "❌ Failed to submit ASR job for: ${file.name}")
                null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in MiraWhisper ASR processing: ${e.message}", e)
            null
        }
    }
    
    /**
     * Check MiraWhisper system status.
     */
    private suspend fun checkMiraWhisperStatus(): Boolean = withContext(Dispatchers.IO) {
        try {
            val miraWhisperRoot = File(MIRAWHISPER_ROOT)
            val modelsDir = File(MODELS_DIR)
            val outputDir = File(OUTPUT_DIR)
            
            val rootExists = miraWhisperRoot.exists()
            val modelsExist = modelsDir.exists()
            val outputExists = outputDir.exists()
            
            Log.d(TAG, "📊 MiraWhisper status:")
            Log.d(TAG, "   Root: $rootExists")
            Log.d(TAG, "   Models: $modelsExist")
            Log.d(TAG, "   Output: $outputExists")
            
            rootExists && modelsExist && outputExists
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking MiraWhisper status: ${e.message}", e)
            false
        }
    }
    
    /**
     * Create scoped ASR job.
     */
    private fun createScopedASRJob(jobId: Int, file: File): String {
        return """
        {
            "id": $jobId,
            "file_id": $jobId,
            "model": "whisper-tiny.en-q5_1",
            "threads": 6,
            "language": "en",
            "translate": false,
            "status": "pending",
            "created_at": "${java.time.Instant.now()}",
            "file_path": "${file.absolutePath}",
            "file_size": ${file.length()},
            "duration": 9.0,
            "sample_rate": 16000,
            "channels": 1,
            "format": "${file.extension}",
            "scoped_storage": true,
            "bridge_service": "ScopedStorageBridgeService"
        }
        """.trimIndent()
    }
    
    /**
     * Submit scoped ASR job to MiraWhisper.
     */
    private suspend fun submitScopedASRJob(asrJob: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📤 Submitting scoped ASR job to MiraWhisper...")
            
            // Write ASR job
            val jobFile = File("$OUTPUT_DIR/scoped_asr_job_${System.currentTimeMillis()}.json")
            jobFile.writeText(asrJob)
            
            // Initiate processing
            // Use scoped storage for processing requests
            val processingRequestFile = File("$MIRAWHISPER_ROOT/scoped_processing_request.txt")
            processingRequestFile.writeText("scoped_processing_requested")
            
            Log.d(TAG, "✅ Scoped ASR job submitted successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error submitting scoped ASR job: ${e.message}", e)
            false
        }
    }
    
    /**
     * Wait for scoped ASR results.
     */
    private suspend fun waitForScopedASRResults(jobId: Int, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "⏱️ Waiting for scoped ASR results for job $jobId...")
            
            // Wait for processing
            kotlinx.coroutines.delay(10000)
            
            // Check for output files
            val outputDir = File(OUTPUT_DIR)
            val outputFiles = outputDir.listFiles()?.filter { 
                it.name.contains(fileName.replace(".mp4", "").replace(".wav", ""))
            } ?: emptyList()
            
            if (outputFiles.isNotEmpty()) {
                val srtFile = outputFiles.find { it.name.endsWith(".srt") }
                if (srtFile != null) {
                    val transcription = srtFile.readText()
                    Log.d(TAG, "✅ Retrieved transcription from: ${srtFile.name}")
                    return@withContext transcription
                }
            }
            
            Log.w(TAG, "⚠️ No transcription results found for: $fileName")
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error waiting for scoped ASR results: ${e.message}", e)
            null
        }
    }
    
    /**
     * Save scoped storage results.
     */
    private suspend fun saveScopedStorageResults(fileName: String, transcription: String, metadata: Map<String, Any>) = 
        withContext(Dispatchers.IO) {
            
            try {
                Log.d(TAG, "💾 Saving scoped storage results for: $fileName")
                
                val baseName = fileName.substringBeforeLast(".")
                val srtFile = File("$OUTPUT_DIR/${baseName}_scoped_bridge.srt")
                val jsonFile = File("$OUTPUT_DIR/${baseName}_scoped_bridge.json")
                
                // Save SRT file
                srtFile.writeText(transcription)
                
                // Save JSON metadata
                val jsonMetadata = """
                {
                    "filename": "$fileName",
                    "processing_method": "ScopedStorageBridgeService",
                    "timestamp": "${java.time.Instant.now()}",
                    "service_version": "1.0",
                    "scoped_storage": true,
                    "metadata": ${metadata.toString()}
                }
                """.trimIndent()
                
                jsonFile.writeText(jsonMetadata)
                
                Log.d(TAG, "✅ Scoped storage results saved:")
                Log.d(TAG, "   📄 SRT: ${srtFile.absolutePath}")
                Log.d(TAG, "   📄 JSON: ${jsonFile.absolutePath}")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error saving scoped storage results: ${e.message}", e)
            }
        }
    
    /**
     * Public method to process a specific file.
     */
    fun processFile(filePath: String) {
        serviceScope.launch {
            val file = File(filePath)
            if (file.exists()) {
                processFileWithScopedStorage(file)
            } else {
                Log.e(TAG, "❌ File does not exist: $filePath")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🛑 ScopedStorageBridgeService destroyed")
    }
}
