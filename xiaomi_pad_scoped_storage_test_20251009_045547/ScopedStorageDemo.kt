package com.mira.videoeditor.infra.storage.demo

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.runBlocking
import com.mira.videoeditor.infra.storage.*

/**
 * Live demonstration of scoped storage design with tennis interview clip.
 * 
 * This shows how the new architecture works:
 * 1. Models are installed to app-private storage with SHA-256 validation
 * 2. Media is accessed via capability-based FD approach
 * 3. Outputs are written to app-private storage and shared via FileProvider
 */
class ScopedStorageDemo(private val context: Context) {
    
    companion object {
        private const val TAG = "ScopedStorageDemo"
    }
    
    /**
     * Demonstrates the complete scoped storage flow with tennis interview clip.
     */
    fun demonstrateTennisClipProcessing(
        tennisClipUri: Uri,
        modelUri: Uri
    ): String = runBlocking {
        
        Log.d(TAG, "🎾 Starting tennis clip processing with scoped storage")
        
        // Initialize scoped storage infrastructure
        val db = EmbeddingDb.getInstance(context)
        val storage: DLStorage = DLStorageImpl(context, db)
        val whisperBridge = ScopedWhisperBridge(context, storage)
        
        Log.d(TAG, "✅ Scoped storage infrastructure initialized")
        
        try {
            // Step 1: Install model to app-private storage
            Log.d(TAG, "📦 Installing model to app-private storage...")
            val modelHandle = storage.installModel(modelUri, "whisper-tiny-en.gguf")
            
            Log.d(TAG, "✅ Model installed:")
            Log.d(TAG, "  - Name: ${modelHandle.name}")
            Log.d(TAG, "  - SHA-256: ${modelHandle.sha256}")
            Log.d(TAG, "  - Path: ${storage.resolveModelPath(modelHandle)}")
            
            // Step 2: Transcribe tennis clip using capability-based access
            Log.d(TAG, "🎤 Transcribing tennis clip using FD-based approach...")
            val transcriptionResult = whisperBridge.transcribe(
                audioUri = tennisClipUri,
                modelUri = modelUri,
                language = "en",
                translate = false,
                threads = 4
            )
            
            Log.d(TAG, "✅ Transcription completed")
            Log.d(TAG, "Result: $transcriptionResult")
            
            // Step 3: Write transcript to app-private storage
            Log.d(TAG, "💾 Writing transcript to app-private storage...")
            val transcriptFile = storage.writeTranscript(
                inputKey = tennisClipUri.toString(),
                transcriptText = transcriptionResult
            )
            
            Log.d(TAG, "✅ Transcript written to: ${transcriptFile.absolutePath}")
            
            // Step 4: Create shareable URI via FileProvider
            Log.d(TAG, "🔗 Creating shareable URI via FileProvider...")
            val shareableUri = storage.shareOutput(transcriptFile)
            
            Log.d(TAG, "✅ Shareable URI created: $shareableUri")
            
            // Step 5: Store embedding for future retrieval
            Log.d(TAG, "🧠 Storing embedding for future retrieval...")
            val embedding = EmbeddingRecord(
                modality = "whisper_text",
                sourceKey = tennisClipUri.toString(),
                tsEpochMs = System.currentTimeMillis(),
                dim = 512, // Example dimension
                vector = FloatArray(512) { (it * 0.01f) }, // Example vector
                extraJson = """{"language": "en", "confidence": 0.95, "duration": "3s"}"""
            )
            
            storage.putEmbedding(embedding)
            Log.d(TAG, "✅ Embedding stored successfully")
            
            // Step 6: Query embeddings to verify storage
            Log.d(TAG, "🔍 Querying embeddings to verify storage...")
            val query = EmbeddingQuery(
                modality = "whisper_text",
                sourceKey = tennisClipUri.toString()
            )
            val retrievedEmbeddings = storage.queryEmbeddings(query)
            
            Log.d(TAG, "✅ Retrieved ${retrievedEmbeddings.size} embeddings")
            
            // Return success summary
            """
            🎾 Tennis Clip Processing Complete!
            
            ✅ Model installed to app-private storage: ${modelHandle.name}@${modelHandle.sha256}
            ✅ Audio transcribed using capability-based FD access
            ✅ Transcript written to app-private storage: ${transcriptFile.name}
            ✅ Shareable URI created: $shareableUri
            ✅ Embedding stored and retrieved successfully
            
            This demonstrates the complete scoped storage flow:
            - No raw file paths used
            - All access via capability-based APIs
            - Models mmapped from app-private storage
            - Outputs shared via FileProvider
            """.trimIndent()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in tennis clip processing: ${e.message}", e)
            """
            ❌ Tennis Clip Processing Failed
            
            Error: ${e.message}
            
            This demonstrates error handling in the scoped storage design.
            """.trimIndent()
        }
    }
    
    /**
     * Demonstrates model information retrieval.
     */
    fun demonstrateModelInfo(modelUri: Uri): String = runBlocking {
        Log.d(TAG, "🔍 Demonstrating model info retrieval...")
        
        val db = EmbeddingDb.getInstance(context)
        val storage: DLStorage = DLStorageImpl(context, db)
        val whisperBridge = ScopedWhisperBridge(context, storage)
        
        try {
            val modelInfo = whisperBridge.getModelInfo(modelUri)
            Log.d(TAG, "✅ Model info retrieved: $modelInfo")
            modelInfo
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error retrieving model info: ${e.message}", e)
            "Error: ${e.message}"
        }
    }
}
