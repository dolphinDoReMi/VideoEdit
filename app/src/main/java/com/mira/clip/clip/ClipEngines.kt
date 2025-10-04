package com.mira.clip.clip

import android.content.Context
import android.graphics.Bitmap
import com.mira.clip.core.Config
import com.mira.clip.ml.PytorchLoader
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.nio.IntBuffer
import kotlin.math.sqrt

/**
 * CLIP engines for text and image encoding.
 * 
 * Provides encodeText() and encodeFrames() with L2-normalized outputs.
 * Output dimension = 512 (ViT-B/32) or 768 (ViT-L/14).
 */
class ClipEngines(private val context: Context) {
    
    private var imageEncoder: Module? = null
    private var textEncoder: Module? = null
    private var tokenizer: ClipBPETokenizer? = null
    
    /**
     * Initialize CLIP encoders and tokenizer.
     */
    fun initialize() {
        try {
            imageEncoder = PytorchLoader.loadImageEncoder(context)
            textEncoder = PytorchLoader.loadTextEncoder(context)
            
            val vocabPath = PytorchLoader.copyBpeVocab(context)
            val mergesPath = PytorchLoader.copyBpeMerges(context)
            tokenizer = ClipBPETokenizer(vocabPath, mergesPath)
            
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize CLIP engines: ${e.message}", e)
        }
    }
    
    /**
     * Encode text to normalized embedding.
     * 
     * @param text Input text
     * @return L2-normalized embedding vector
     */
    fun encodeText(text: String): FloatArray {
        val encoder = textEncoder ?: throw IllegalStateException("Text encoder not initialized")
        val tokenizer = this.tokenizer ?: throw IllegalStateException("Tokenizer not initialized")
        
        // Tokenize text
        val tokens = tokenizer.tokenize(text)
        val tokenTensor = Tensor.fromBlob(
            IntBuffer.wrap(tokens),
            longArrayOf(1, tokens.size.toLong())
        )
        
        // Encode with CLIP text encoder
        val result = encoder.forward(IValue.from(tokenTensor as Tensor))
        val embedding = result.toTensor().dataAsFloatArray
        
        // Apply mean pooling and L2 normalization
        return normalizeEmbedding(embedding)
    }
    
    /**
     * Encode video frames to normalized embedding.
     * 
     * @param frames List of video frames (bitmaps)
     * @return L2-normalized embedding vector
     */
    fun encodeFrames(frames: List<Bitmap>): FloatArray {
        val encoder = imageEncoder ?: throw IllegalStateException("Image encoder not initialized")
        
        if (frames.isEmpty()) {
            throw IllegalArgumentException("Frames list cannot be empty")
        }
        
        // Process each frame
        val frameEmbeddings = mutableListOf<FloatArray>()
        
        for (frame in frames) {
            // Preprocess frame
            val preprocessed = ClipPreprocess.preprocessImage(frame)
            
            // Encode with CLIP image encoder
            val result = encoder.forward(IValue.from(preprocessed as Tensor))
            val embedding = result.toTensor().dataAsFloatArray
            
            // Normalize individual frame embedding
            frameEmbeddings.add(normalizeEmbedding(embedding))
        }
        
        // Apply mean pooling across frames
        val meanEmbedding = meanPoolEmbeddings(frameEmbeddings)
        
        // Final L2 normalization
        return normalizeEmbedding(meanEmbedding)
    }
    
    /**
     * Apply L2 normalization to embedding vector.
     */
    private fun normalizeEmbedding(embedding: FloatArray): FloatArray {
        val norm = sqrt(embedding.sumOf { it.toDouble() * it.toDouble() }.toFloat())
        
        if (norm == 0f) {
            return FloatArray(embedding.size) { 0f }
        }
        
        return embedding.map { it / norm.toFloat() }.toFloatArray()
    }
    
    /**
     * Apply mean pooling across multiple embeddings.
     */
    private fun meanPoolEmbeddings(embeddings: List<FloatArray>): FloatArray {
        if (embeddings.isEmpty()) {
            throw IllegalArgumentException("Cannot pool empty embeddings list")
        }
        
        val dimension = embeddings[0].size
        val pooled = FloatArray(dimension)
        
        for (embedding in embeddings) {
            for (i in 0 until dimension) {
                pooled[i] += embedding[i]
            }
        }
        
        val count = embeddings.size.toFloat()
        for (i in 0 until dimension) {
            pooled[i] /= count
        }
        
        return pooled
    }
    
    /**
     * Get expected embedding dimension for current variant.
     */
    fun getEmbeddingDimension(): Int {
        return when (Config.DEFAULT_VARIANT) {
            "clip_vit_b32_mean_v1" -> Config.EMBEDDING_DIM_VIT_B32
            "clip_vit_l14_mean_v1" -> Config.EMBEDDING_DIM_VIT_L14
            else -> Config.EMBEDDING_DIM_VIT_B32
        }
    }
    
    companion object {
        /**
         * Static convenience method for text embedding.
         * Creates a temporary ClipEngines instance for one-time use.
         */
        fun embedText(context: Context, text: String): FloatArray {
            val engines = ClipEngines(context)
            engines.initialize()
            return engines.encodeText(text)
        }
        
        /**
         * Static convenience method for single image embedding.
         * Creates a temporary ClipEngines instance for one-time use.
         */
        fun embedImage(context: Context, bitmap: android.graphics.Bitmap): FloatArray {
            val engines = ClipEngines(context)
            engines.initialize()
            return engines.encodeFrames(listOf(bitmap))
        }
        
        /**
         * Static convenience method for L2 normalization.
         * Useful for testing and manual vector operations.
         */
        fun normalizeEmbedding(embedding: FloatArray): FloatArray {
            val norm = sqrt(embedding.sumOf { it.toDouble() * it.toDouble() }.toFloat())
            
            if (norm == 0f) {
                return FloatArray(embedding.size) { 0f }
            }
            
            return embedding.map { it / norm.toFloat() }.toFloatArray()
        }
    }
}
