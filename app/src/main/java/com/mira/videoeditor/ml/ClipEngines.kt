package com.mira.videoeditor.ml

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.runBlocking

/**
 * ClipEngines - Unified interface for CLIP model operations
 * 
 * Provides a simplified interface for image and text encoding using CLIP models.
 * This class wraps the PyTorchClipEngine and provides the interface expected by tests.
 */
class ClipEngines(private val context: Context) {
    
    private val pytorchEngine = PyTorchClipEngine(context)
    private var isInitialized = false
    
    /**
     * Initialize the CLIP engines
     */
    suspend fun initialize(): Boolean {
        if (!isInitialized) {
            isInitialized = pytorchEngine.initialize()
        }
        return isInitialized
    }
    
    /**
     * Encode frames (images) using CLIP image encoder
     * 
     * @param bitmaps List of bitmaps to encode
     * @param batch Batch size for processing (default 1)
     * @return List of normalized embeddings
     */
    fun encodeFrames(bitmaps: List<Bitmap>, batch: Int = 1): FloatArray {
        if (!isInitialized) {
            throw IllegalStateException("ClipEngines not initialized")
        }
        
        return if (bitmaps.size == 1) {
            pytorchEngine.encodeImage(bitmaps.first())
        } else {
            pytorchEngine.encodeImages(bitmaps).first()
        }
    }
    
    /**
     * Encode text using CLIP text encoder
     * 
     * @param text Text to encode
     * @return Normalized text embedding
     */
    fun encodeText(text: String): FloatArray {
        if (!isInitialized) {
            throw IllegalStateException("ClipEngines not initialized")
        }
        
        return pytorchEngine.encodeText(text)
    }
    
    /**
     * Check if engines are ready for use
     */
    fun isReady(): Boolean = isInitialized && pytorchEngine.isReady()
    
    /**
     * Compute cosine similarity between two embeddings
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        return pytorchEngine.cosineSimilarity(a, b)
    }
}
