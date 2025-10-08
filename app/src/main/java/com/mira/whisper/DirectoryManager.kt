package com.mira.whisper

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * Directory Manager for App-Scoped Storage
 * 
 * Implements the directory structure specified in Device Deployment documentation:
 * /sdcard/Mira/
 * ├── inbox/                    # Input directory
 * │   ├── audio/               # Audio files (.wav, .mp4)
 * │   ├── video/               # Video files (.mp4, .mov)
 * │   └── batch/               # Batch processing files
 * ├── processing/              # Active processing directory
 * │   ├── temp/               # Temporary files during processing
 * │   ├── chunks/             # Audio/video chunks
 * │   └── intermediate/       # Intermediate processing files
 * ├── output/                  # Output directory
 * │   ├── transcripts/        # Whisper transcription results
 * │   ├── clips/              # AutoClipper video clips
 * │   ├── metadata/           # Processing metadata
 * │   └── exports/            # Exported files (JSON, SRT, TXT)
 * ├── models/                  # ML models storage
 * │   ├── whisper/            # Whisper model files
 * │   └── clip/               # CLIP model files
 * └── logs/                    # System logs
 *     ├── whisper/            # Whisper processing logs
 *     ├── autoclip/           # AutoClipper service logs
 *     └── system/             # System resource logs
 */
object DirectoryManager {
    
    private const val TAG = "DirectoryManager"
    
    // Base directory for all Mira operations
    private const val MIRA_BASE_DIR = "Mira"
    
    // Directory structure constants
    private const val INBOX_DIR = "inbox"
    private const val PROCESSING_DIR = "processing"
    private const val OUTPUT_DIR = "output"
    private const val MODELS_DIR = "models"
    private const val LOGS_DIR = "logs"
    
    // Subdirectory constants
    private const val AUDIO_DIR = "audio"
    private const val VIDEO_DIR = "video"
    private const val BATCH_DIR = "batch"
    private const val TEMP_DIR = "temp"
    private const val CHUNKS_DIR = "chunks"
    private const val INTERMEDIATE_DIR = "intermediate"
    private const val TRANSCRIPTS_DIR = "transcripts"
    private const val CLIPS_DIR = "clips"
    private const val METADATA_DIR = "metadata"
    private const val EXPORTS_DIR = "exports"
    private const val WHISPER_MODELS_DIR = "whisper"
    private const val CLIP_MODELS_DIR = "clip"
    private const val WHISPER_LOGS_DIR = "whisper"
    private const val AUTOCLIP_LOGS_DIR = "autoclip"
    private const val SYSTEM_LOGS_DIR = "system"
    
    /**
     * Get the base Mira directory using app-scoped storage
     * Uses getExternalFilesDir() which requires no runtime permissions
     */
    fun getMiraBaseDir(context: Context): File {
        return File(context.getExternalFilesDir(null), MIRA_BASE_DIR)
    }
    
    /**
     * Get the inbox directory for input files
     */
    fun getInboxDir(context: Context): File {
        return File(getMiraBaseDir(context), INBOX_DIR)
    }
    
    /**
     * Get the audio inbox directory
     */
    fun getAudioInboxDir(context: Context): File {
        return File(getInboxDir(context), AUDIO_DIR)
    }
    
    /**
     * Get the video inbox directory
     */
    fun getVideoInboxDir(context: Context): File {
        return File(getInboxDir(context), VIDEO_DIR)
    }
    
    /**
     * Get the batch inbox directory
     */
    fun getBatchInboxDir(context: Context): File {
        return File(getInboxDir(context), BATCH_DIR)
    }
    
    /**
     * Get the processing directory for active processing
     */
    fun getProcessingDir(context: Context): File {
        return File(getMiraBaseDir(context), PROCESSING_DIR)
    }
    
    /**
     * Get the temp processing directory
     */
    fun getTempProcessingDir(context: Context): File {
        return File(getProcessingDir(context), TEMP_DIR)
    }
    
    /**
     * Get the chunks processing directory
     */
    fun getChunksProcessingDir(context: Context): File {
        return File(getProcessingDir(context), CHUNKS_DIR)
    }
    
    /**
     * Get the intermediate processing directory
     */
    fun getIntermediateProcessingDir(context: Context): File {
        return File(getProcessingDir(context), INTERMEDIATE_DIR)
    }
    
    /**
     * Get the output directory for results
     */
    fun getOutputDir(context: Context): File {
        return File(getMiraBaseDir(context), OUTPUT_DIR)
    }
    
    /**
     * Get the transcripts output directory
     */
    fun getTranscriptsOutputDir(context: Context): File {
        return File(getOutputDir(context), TRANSCRIPTS_DIR)
    }
    
    /**
     * Get the clips output directory
     */
    fun getClipsOutputDir(context: Context): File {
        return File(getOutputDir(context), CLIPS_DIR)
    }
    
    /**
     * Get the metadata output directory
     */
    fun getMetadataOutputDir(context: Context): File {
        return File(getOutputDir(context), METADATA_DIR)
    }
    
    /**
     * Get the exports output directory
     */
    fun getExportsOutputDir(context: Context): File {
        return File(getOutputDir(context), EXPORTS_DIR)
    }
    
    /**
     * Get the models directory
     */
    fun getModelsDir(context: Context): File {
        return File(getMiraBaseDir(context), MODELS_DIR)
    }
    
    /**
     * Get the whisper models directory
     */
    fun getWhisperModelsDir(context: Context): File {
        return File(getModelsDir(context), WHISPER_MODELS_DIR)
    }
    
    /**
     * Get the CLIP models directory
     */
    fun getClipModelsDir(context: Context): File {
        return File(getModelsDir(context), CLIP_MODELS_DIR)
    }
    
    /**
     * Get the logs directory
     */
    fun getLogsDir(context: Context): File {
        return File(getMiraBaseDir(context), LOGS_DIR)
    }
    
    /**
     * Get the whisper logs directory
     */
    fun getWhisperLogsDir(context: Context): File {
        return File(getLogsDir(context), WHISPER_LOGS_DIR)
    }
    
    /**
     * Get the autoclip logs directory
     */
    fun getAutoclipLogsDir(context: Context): File {
        return File(getLogsDir(context), AUTOCLIP_LOGS_DIR)
    }
    
    /**
     * Get the system logs directory
     */
    fun getSystemLogsDir(context: Context): File {
        return File(getLogsDir(context), SYSTEM_LOGS_DIR)
    }
    
    /**
     * Ensure all directories exist and are writable
     * Creates the complete directory structure if it doesn't exist
     */
    fun ensureDirectoryStructure(context: Context): Boolean {
        return try {
            val directories = listOf(
                getMiraBaseDir(context),
                getInboxDir(context),
                getAudioInboxDir(context),
                getVideoInboxDir(context),
                getBatchInboxDir(context),
                getProcessingDir(context),
                getTempProcessingDir(context),
                getChunksProcessingDir(context),
                getIntermediateProcessingDir(context),
                getOutputDir(context),
                getTranscriptsOutputDir(context),
                getClipsOutputDir(context),
                getMetadataOutputDir(context),
                getExportsOutputDir(context),
                getModelsDir(context),
                getWhisperModelsDir(context),
                getClipModelsDir(context),
                getLogsDir(context),
                getWhisperLogsDir(context),
                getAutoclipLogsDir(context),
                getSystemLogsDir(context)
            )
            
            directories.forEach { dir ->
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        android.util.Log.e(TAG, "Failed to create directory: ${dir.absolutePath}")
                        return false
                    }
                }
                
                // Verify directory is writable
                if (!dir.canWrite()) {
                    android.util.Log.e(TAG, "Directory not writable: ${dir.absolutePath}")
                    return false
                }
            }
            
            android.util.Log.i(TAG, "Directory structure created successfully")
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error creating directory structure: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get a job-specific output directory for transcripts
     */
    fun getJobTranscriptsDir(context: Context, jobId: String): File {
        return File(getTranscriptsOutputDir(context), jobId)
    }
    
    /**
     * Get a job-specific output directory for clips
     */
    fun getJobClipsDir(context: Context, jobId: String): File {
        return File(getClipsOutputDir(context), jobId)
    }
    
    /**
     * Get a job-specific metadata directory
     */
    fun getJobMetadataDir(context: Context, jobId: String): File {
        return File(getMetadataOutputDir(context), jobId)
    }
    
    /**
     * Get the default whisper model path
     */
    fun getDefaultWhisperModelPath(context: Context): String {
        return File(getWhisperModelsDir(context), "whisper-base.q5_1.bin").absolutePath
    }
    
    /**
     * Get the default CLIP model path
     */
    fun getDefaultClipModelPath(context: Context): String {
        return File(getClipModelsDir(context), "clip-model.bin").absolutePath
    }
    
    /**
     * Clean up temporary files from processing directory
     */
    fun cleanupTempFiles(context: Context): Boolean {
        return try {
            val tempDir = getTempProcessingDir(context)
            if (tempDir.exists()) {
                tempDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                    }
                }
            }
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error cleaning up temp files: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get directory structure info for debugging
     */
    fun getDirectoryInfo(context: Context): Map<String, String> {
        return mapOf(
            "baseDir" to getMiraBaseDir(context).absolutePath,
            "inboxDir" to getInboxDir(context).absolutePath,
            "audioInboxDir" to getAudioInboxDir(context).absolutePath,
            "videoInboxDir" to getVideoInboxDir(context).absolutePath,
            "processingDir" to getProcessingDir(context).absolutePath,
            "outputDir" to getOutputDir(context).absolutePath,
            "transcriptsDir" to getTranscriptsOutputDir(context).absolutePath,
            "clipsDir" to getClipsOutputDir(context).absolutePath,
            "modelsDir" to getModelsDir(context).absolutePath,
            "whisperModelsDir" to getWhisperModelsDir(context).absolutePath,
            "logsDir" to getLogsDir(context).absolutePath
        )
    }
}
