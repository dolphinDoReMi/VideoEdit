package com.mira.videoeditor.mira.storage

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Mira Storage Service that demonstrates proper Android 11+ compliant file operations
 * using the mira-storage components.
 */
class MiraStorageService(
    private val context: Context
) {
    companion object {
        private const val TAG = "MiraStorageService"
    }
    
    private val storage: MiraDLStorage = AndroidMiraDLStorage(context)
    private val media: MiraMediaProcessing = AndroidMiraMediaProcessing(context)
    // Note: MiraWhisperEngine will be injected by the app module to avoid circular dependencies
    private var whisperEngine: MiraWhisperEngine? = null
    
    /**
     * Set the Whisper engine from the app module.
     * This avoids circular dependencies between infra-storage and whisper-native.
     */
    fun setWhisperEngine(engine: MiraWhisperEngine) {
        this.whisperEngine = engine
    }
    
    /**
     * Validate content URI for scoped storage compliance.
     */
    private fun validateContentUri(uri: Uri) {
        when {
            uri.scheme == "content" -> {
                // Valid content URI
                Log.d(TAG, "Valid content URI: $uri")
            }
            uri.scheme == "file" -> {
                // File URI - check if it's in app-private storage
                val path = uri.path ?: throw IllegalArgumentException("Invalid file URI: $uri")
                val appPrivateDir = context.filesDir.absolutePath
                if (!path.startsWith(appPrivateDir)) {
                    throw SecurityException("File URI outside app-private storage: $uri")
                }
                Log.d(TAG, "Valid app-private file URI: $uri")
            }
            uri.scheme == "android.resource" -> {
                // Android resource URI - valid for assets
                Log.d(TAG, "Valid Android resource URI: $uri")
            }
            else -> {
                throw IllegalArgumentException("Unsupported URI scheme: ${uri.scheme}")
            }
        }
    }
    
    /**
     * Process a video file using scoped storage access patterns.
     * This demonstrates the complete pipeline from file access to transcription.
     */
    suspend fun processVideoWithScopedStorage(videoUri: Uri, modelUri: Uri): ProcessingResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting scoped storage processing for video: $videoUri")
                
                // Validate URIs before processing
                validateContentUri(videoUri)
                validateContentUri(modelUri)
                
                // Step 1: Get media information using scoped access
                val mediaInfo = getMediaInfoScoped(videoUri)
                Log.d(TAG, "Media info: duration=${mediaInfo.duration}s, sampleRate=${mediaInfo.sampleRate}, channels=${mediaInfo.channels}")
                
                // Step 2: Install model using scoped storage
                val modelHandle = storage.installModel(modelUri, "whisper-model.gguf")
                Log.d(TAG, "Model installed: ${modelHandle.name}, size=${modelHandle.size}, sha256=${modelHandle.sha256}")
                
                // Step 3: Extract audio using scoped access
                val audioData = extractAudioScoped(videoUri)
                Log.d(TAG, "Audio extracted: ${audioData.size} bytes")
                
                // Step 4: Transcribe using WhisperEngine with scoped storage
                if (whisperEngine == null) {
                    throw IllegalStateException("Whisper engine not initialized. Call setWhisperEngine() first.")
                }
                
                val transcript = whisperEngine!!.transcribe(videoUri, modelUri, "en", false, 4)
                Log.d(TAG, "Transcription completed: ${transcript.length} characters")
                
                // Step 5: Save results using scoped storage
                val transcriptUri = whisperEngine!!.writeTranscript("tennis_interview_clip_002", transcript)
                Log.d(TAG, "Transcript saved: $transcriptUri")
                
                ProcessingResult(
                    success = true,
                    transcript = transcript,
                    transcriptUri = Uri.parse(transcriptUri),
                    mediaInfo = mediaInfo,
                    modelHandle = modelHandle
                )
                
            } catch (e: SecurityException) {
                Log.e(TAG, "Security error in scoped storage processing", e)
                ProcessingResult(
                    success = false,
                    error = "Security error: ${e.message}",
                    transcript = null,
                    transcriptUri = null,
                    mediaInfo = null,
                    modelHandle = null
                )
            } catch (e: IllegalStateException) {
                Log.e(TAG, "State error in scoped storage processing", e)
                ProcessingResult(
                    success = false,
                    error = "State error: ${e.message}",
                    transcript = null,
                    transcriptUri = null,
                    mediaInfo = null,
                    modelHandle = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Scoped storage processing failed", e)
                ProcessingResult(
                    success = false,
                    error = e.message ?: "Unknown error",
                    transcript = null,
                    transcriptUri = null,
                    mediaInfo = null,
                    modelHandle = null
                )
            }
        }
    }
    
    /**
     * Get media information using scoped storage access.
     */
    private suspend fun getMediaInfoScoped(uri: Uri): MediaInfo {
        val contentResolver = context.contentResolver
        val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("Cannot open file descriptor for URI: $uri")
        
        return try {
            media.getMediaInfo(parcelFileDescriptor.fileDescriptor)
        } finally {
            parcelFileDescriptor.close()
        }
    }
    
    /**
     * Extract audio using scoped storage access.
     */
    private suspend fun extractAudioScoped(uri: Uri): ByteArray {
        val contentResolver = context.contentResolver
        val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalStateException("Cannot open file descriptor for URI: $uri")
        
        return try {
            media.extractAudioFromVideo(parcelFileDescriptor.fileDescriptor, 16000, true)
        } finally {
            parcelFileDescriptor.close()
        }
    }
    
    /**
     * Check if a model exists in app-private storage using scoped storage.
     */
    suspend fun checkModelExists(modelName: String): Boolean {
        return try {
            val modelFile = File(context.filesDir, "whisper_models/$modelName")
            modelFile.exists()
        } catch (e: Exception) {
            Log.w(TAG, "Error checking model existence: ${e.message}")
            false
        }
    }
    
    /**
     * List available models in app-private storage using scoped storage.
     */
    suspend fun listAvailableModels(): List<String> {
        return try {
            val modelsDir = File(context.filesDir, "whisper_models")
            if (modelsDir.exists()) {
                modelsDir.listFiles()?.map { it.name } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error listing models: ${e.message}")
            emptyList()
        }
    }

    /**
     * Find video file using MediaStore with proper scoped storage access.
     * Uses DISPLAY_NAME instead of DATA field for Android 11+ compliance.
     */
    suspend fun findVideoFile(fileName: String): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.SIZE
                )
                val selection = "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
                val selectionArgs = arrayOf("%$fileName%")

                val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val idIndex = it.getColumnIndex(MediaStore.Video.Media._ID)
                        val id = it.getLong(idIndex)
                        val foundUri = Uri.withAppendedPath(uri, id.toString())
                        Log.d(TAG, "Found video file: $foundUri")
                        return@withContext foundUri
                    }
                }
                Log.w(TAG, "Video file not found: $fileName")
                null
            } catch (e: Exception) {
                Log.e(TAG, "Error finding video file: ${e.message}")
                null
            }
        }
    }

    /**
     * Find model file using MediaStore with proper scoped storage access.
     */
    suspend fun findModelFile(fileName: String): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Files.getContentUri("external")
                }
                val projection = arrayOf(
                    MediaStore.Downloads._ID,
                    MediaStore.Downloads.DISPLAY_NAME,
                    MediaStore.Downloads.SIZE
                )
                val selection = "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
                val selectionArgs = arrayOf("%$fileName%")

                val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val idIndex = it.getColumnIndex(MediaStore.Downloads._ID)
                        val id = it.getLong(idIndex)
                        val foundUri = Uri.withAppendedPath(uri, id.toString())
                        Log.d(TAG, "Found model file: $foundUri")
                        return@withContext foundUri
                    }
                }
                Log.w(TAG, "Model file not found: $fileName")
                null
            } catch (e: Exception) {
                Log.e(TAG, "Error finding model file: ${e.message}")
                null
            }
        }
    }

    /**
     * Find model file in app assets as fallback.
     */
    suspend fun findModelInAssets(fileName: String): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val assetsManager = context.assets
                val assetPath = "models/$fileName"
                
                // Check if the asset exists
                val inputStream = assetsManager.open(assetPath)
                inputStream.close()
                
                // Create a content URI for the asset
                val assetUri = Uri.parse("android.resource://${context.packageName}/raw/$fileName")
                Log.d(TAG, "Found model in assets: $assetUri")
                assetUri
            } catch (e: Exception) {
                Log.w(TAG, "Model not found in assets: $fileName")
                null
            }
        }
    }

    /**
     * Find model file with fallback to assets.
     */
    suspend fun findModelFileWithFallback(fileName: String): Uri? {
        // First try MediaStore (Downloads)
        val mediaStoreUri = findModelFile(fileName)
        if (mediaStoreUri != null) {
            return mediaStoreUri
        }
        
        // Fallback to assets
        return findModelInAssets(fileName)
    }

    /**
     * Get file information using MediaStore with proper scoped storage access.
     */
    suspend fun getFileInfo(uri: Uri): FileInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val projection = arrayOf(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.SIZE
                )

                val cursor = contentResolver.query(uri, projection, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                        val sizeIndex = it.getColumnIndex(MediaStore.MediaColumns.SIZE)
                        
                        val name = if (nameIndex >= 0) it.getString(nameIndex) else "Unknown"
                        val size = if (sizeIndex >= 0) it.getLong(sizeIndex) else 0L
                        
                        val fileInfo = FileInfo(name, size, uri.toString())
                        Log.d(TAG, "File info: $fileInfo")
                        return@withContext fileInfo
                    }
                }
                Log.w(TAG, "Could not get file info for: $uri")
                null
            } catch (e: Exception) {
                Log.e(TAG, "Error getting file info: ${e.message}")
                null
            }
        }
    }
    
    /**
     * Demonstrate proper scoped storage access pattern as described in the analysis.
     * This method shows the correct way to access video files using MediaStore APIs.
     */
    suspend fun demonstrateScopedStoragePattern(fileName: String): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(MediaStore.Video.Media._ID)
                val selection = "${MediaStore.Video.Media.DISPLAY_NAME} = ?"
                val selectionArgs = arrayOf(fileName)

                val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                        val foundUri = Uri.withAppendedPath(uri, id.toString())
                        
                        // Demonstrate proper file descriptor access
                        val parcelFileDescriptor = contentResolver.openFileDescriptor(foundUri, "r")
                        if (parcelFileDescriptor != null) {
                            try {
                                val fd = parcelFileDescriptor.fileDescriptor
                                Log.d(TAG, "Successfully opened file descriptor for scoped storage access: $foundUri")
                                
                                // This is the proper way to use the file descriptor with MediaExtractor
                                // as mentioned in the analysis: MediaExtractor().apply { setDataSource(fd) }
                                return@withContext foundUri
                            } finally {
                                parcelFileDescriptor.close()
                            }
                        }
                    }
                }
                Log.w(TAG, "Video file not found: $fileName")
                null
            } catch (e: Exception) {
                Log.e(TAG, "Error in scoped storage demonstration: ${e.message}")
                null
            }
        }
    }
}

/**
 * Result of scoped storage processing.
 */
data class ProcessingResult(
    val success: Boolean,
    val transcript: String? = null,
    val transcriptUri: Uri? = null,
    val mediaInfo: MediaInfo? = null,
    val modelHandle: MiraDLStorage.ModelHandle? = null,
    val error: String? = null
)

/**
 * File information from MediaStore.
 */
data class FileInfo(
    val name: String,
    val size: Long,
    val uri: String
)
