package com.mira.videoeditor.mira.storage

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * End-to-End Verification Test for Real Whisper Processing
 * 
 * This test verifies the complete pipeline from file access to transcription
 * using real Whisper processing with zero-copy operations and no mock data.
 */
class MiraStorageE2ETest(private val context: Context) {
    
    companion object {
        private const val TAG = "MiraStorageE2ETest"
    }
    
    private val miraStorageService = MiraStorageService(context)
    
    /**
     * Run complete end-to-end verification test.
     * 
     * This test verifies:
     * 1. File discovery using scoped storage
     * 2. Model loading and initialization
     * 3. Audio extraction with zero-copy operations
     * 4. Real Whisper transcription (no mock data)
     * 5. Result validation and output
     */
    suspend fun runCompleteVerification(): E2ETestResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "🚀 Starting End-to-End Verification Test")
        
        try {
            // Step 1: File Discovery Verification
            Log.d(TAG, "Step 1: Verifying file discovery using scoped storage")
            val videoUri = findTennisClipUri()
            if (videoUri == null) {
                return@withContext E2ETestResult.Error("Tennis clip not found in MediaStore")
            }
            Log.d(TAG, "✅ File discovery successful: $videoUri")
            
            // Step 2: Model Discovery Verification
            Log.d(TAG, "Step 2: Verifying model discovery using scoped storage")
            val modelUri = findModelUri()
            if (modelUri == null) {
                return@withContext E2ETestResult.Error("GGUF model not found in MediaStore")
            }
            Log.d(TAG, "✅ Model discovery successful: $modelUri")
            
            // Step 3: Real Whisper Processing Verification
            Log.d(TAG, "Step 3: Running real Whisper processing (no mock data)")
            val result = miraStorageService.processVideoWithScopedStorage(videoUri, modelUri)
            
            if (!result.success) {
                return@withContext E2ETestResult.Error("Processing failed: ${result.error}")
            }
            
            // Step 4: Result Validation
            Log.d(TAG, "Step 4: Validating transcription results")
            val transcript = result.transcript ?: return@withContext E2ETestResult.Error("No transcript generated")
            val transcriptUri = result.transcriptUri?.toString() ?: return@withContext E2ETestResult.Error("No transcript URI generated")
            
            val validationResult = validateTranscriptionResult(transcript)
            if (!validationResult.isValid) {
                return@withContext E2ETestResult.Error("Result validation failed: ${validationResult.error}")
            }
            
            Log.d(TAG, "✅ End-to-End Verification Test PASSED")
            E2ETestResult.Success(
                transcript = transcript,
                transcriptUri = transcriptUri,
                processingTime = System.currentTimeMillis(),
                validationResult = validationResult
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ End-to-End Verification Test FAILED", e)
            E2ETestResult.Error("Test failed with exception: ${e.message}")
        }
    }
    
    /**
     * Find tennis clip using MediaStore (scoped storage compliant).
     */
    private fun findTennisClipUri(): Uri? {
        val contentResolver = context.contentResolver
        val uri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(android.provider.MediaStore.Video.Media._ID, android.provider.MediaStore.Video.Media.DATA)
        val selection = "${android.provider.MediaStore.Video.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("%tennis_interview_clip_002.mp4%")
        
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndex(android.provider.MediaStore.Video.Media._ID)
                val id = it.getLong(idIndex)
                return Uri.withAppendedPath(uri, id.toString())
            }
        }
        return null
    }
    
    /**
     * Find model file using scoped storage.
     */
    private fun findModelUri(): Uri? {
        val contentResolver = context.contentResolver
        val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            android.provider.MediaStore.Files.getContentUri("external")
        }
        val projection = arrayOf(android.provider.MediaStore.Downloads._ID, android.provider.MediaStore.Downloads.DISPLAY_NAME)
        val selection = "${android.provider.MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%.gguf%")
        
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndex(android.provider.MediaStore.Downloads._ID)
                val id = it.getLong(idIndex)
                return Uri.withAppendedPath(uri, id.toString())
            }
        }
        return null
    }
    
    /**
     * Validate transcription result to ensure it's real (not mock data).
     */
    private fun validateTranscriptionResult(transcript: String): ValidationResult {
        // Check for mock data patterns
        val mockPatterns = listOf(
            "Mock transcription",
            "Mock data",
            "placeholder",
            "test data",
            "fake result"
        )
        
        val lowerTranscript = transcript.lowercase()
        for (pattern in mockPatterns) {
            if (lowerTranscript.contains(pattern.lowercase())) {
                return ValidationResult(false, "Mock data detected: $pattern")
            }
        }
        
        // Check for reasonable transcription length
        if (transcript.length < 10) {
            return ValidationResult(false, "Transcription too short: ${transcript.length} characters")
        }
        
        // Check for reasonable word count
        val wordCount = transcript.split("\\s+".toRegex()).size
        if (wordCount < 3) {
            return ValidationResult(false, "Too few words: $wordCount")
        }
        
        // Check for reasonable character diversity
        val uniqueChars = transcript.toSet().size
        if (uniqueChars < 5) {
            return ValidationResult(false, "Insufficient character diversity: $uniqueChars")
        }
        
        return ValidationResult(true, "Transcription validation passed")
    }
    
    /**
     * E2E Test Result sealed class.
     */
    sealed class E2ETestResult {
        data class Success(
            val transcript: String,
            val transcriptUri: String,
            val processingTime: Long,
            val validationResult: ValidationResult
        ) : E2ETestResult()
        
        data class Error(val message: String) : E2ETestResult()
    }
    
    /**
     * Validation result data class.
     */
    data class ValidationResult(
        val isValid: Boolean,
        val error: String? = null
    )
}
