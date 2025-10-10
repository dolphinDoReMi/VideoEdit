package com.mira.videoeditor.mira.storage

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Capability-based storage abstraction for scoped storage compliance.
 * 
 * This interface provides Android 11+ scoped storage compliant operations
 * for model installation, file access, and output sharing.
 */
interface MiraDLStorage {
    
    /**
     * Model handle containing metadata about installed models.
     */
    data class ModelHandle(
        val name: String,
        val sha256: String,
        val size: Long,
        val installedAt: Long
    )
    
    /**
     * Installs a model from a URI to app-private storage.
     * 
     * @param modelUri URI to the model file
     * @param targetName Target filename in app-private storage
     * @return ModelHandle with metadata about the installed model
     */
    suspend fun installModel(modelUri: Uri, targetName: String): ModelHandle
    
    /**
     * Resolves the app-private file path for a model handle.
     * 
     * @param modelHandle The model handle
     * @return File path to the installed model
     */
    fun resolveModelPath(modelHandle: ModelHandle): String
    
    /**
     * Opens a file descriptor for reading from a URI.
     * 
     * @param uri URI to the file
     * @return File descriptor for reading
     */
    suspend fun openReadFd(uri: Uri): java.io.FileDescriptor
    
    /**
     * Writes a transcript to app-private storage.
     * 
     * @param inputKey Stable key identifying the input
     * @param transcriptText Transcription text
     * @return File path to the written transcript
     */
    suspend fun writeTranscript(inputKey: String, transcriptText: String): String
    
    /**
     * Writes a preview image to app-private storage.
     * 
     * @param inputKey Stable key identifying the input
     * @param imageBytes Image data
     * @return File path to the written image
     */
    suspend fun writePreviewImage(inputKey: String, imageBytes: ByteArray): String
    
    /**
     * Creates a shareable URI for an output file.
     * 
     * @param filePath Path to the file in app-private storage
     * @return Shareable URI for the file
     */
    suspend fun shareOutput(filePath: String): Uri
}

/**
 * Android implementation of DLStorage using scoped storage APIs.
 */
class AndroidMiraDLStorage(
    private val context: Context
) : MiraDLStorage {
    
    companion object {
        private const val TAG = "AndroidMiraDLStorage"
        private const val MODELS_DIR = "whisper_models"
        private const val OUTPUTS_DIR = "whisper_outputs"
    }
    
    private val modelsDir = File(context.filesDir, MODELS_DIR).apply { 
        if (!exists()) mkdirs() 
    }
    
    private val outputsDir = File(context.filesDir, OUTPUTS_DIR).apply { 
        if (!exists()) mkdirs() 
    }
    
    override suspend fun installModel(modelUri: Uri, targetName: String): MiraDLStorage.ModelHandle {
        val contentResolver = context.contentResolver
        val targetFile = File(modelsDir, targetName)
        
        try {
            // Read model data from URI
            val inputStream = contentResolver.openInputStream(modelUri)
                ?: throw IllegalStateException("Cannot open input stream for model URI: $modelUri")
            
            // Write to app-private storage
            val outputStream = targetFile.outputStream()
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            // Calculate SHA256 hash
            val sha256 = targetFile.inputStream().use { input ->
                java.security.MessageDigest.getInstance("SHA-256").let { digest ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                    }
                    digest.digest().joinToString("") { "%02x".format(it) }
                }
            }
            
            return MiraDLStorage.ModelHandle(
                name = targetName,
                sha256 = sha256,
                size = targetFile.length(),
                installedAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            throw IllegalStateException("Failed to install model from URI: $modelUri", e)
        }
    }
    
    override fun resolveModelPath(modelHandle: MiraDLStorage.ModelHandle): String {
        return File(modelsDir, modelHandle.name).absolutePath
    }
    
    override suspend fun openReadFd(uri: Uri): java.io.FileDescriptor {
        val contentResolver = context.contentResolver
        
        try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
                ?: throw IllegalStateException("Cannot open file descriptor for URI: $uri")
            
            return parcelFileDescriptor.fileDescriptor
        } catch (e: Exception) {
            throw IllegalStateException("Failed to open file descriptor for URI: $uri", e)
        }
    }
    
    override suspend fun writeTranscript(inputKey: String, transcriptText: String): String {
        val transcriptFile = File(outputsDir, "${inputKey}_transcript.txt")
        transcriptFile.writeText(transcriptText)
        return transcriptFile.absolutePath
    }
    
    override suspend fun writePreviewImage(inputKey: String, imageBytes: ByteArray): String {
        val imageFile = File(outputsDir, "${inputKey}_preview.jpg")
        imageFile.writeBytes(imageBytes)
        return imageFile.absolutePath
    }
    
    override suspend fun shareOutput(filePath: String): Uri {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalStateException("File does not exist: $filePath")
        }
        
        try {
            // Create a content URI using FileProvider
            val authority = "${context.packageName}.fileprovider"
            return androidx.core.content.FileProvider.getUriForFile(context, authority, file)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to create shareable URI for file: $filePath", e)
        }
    }
}