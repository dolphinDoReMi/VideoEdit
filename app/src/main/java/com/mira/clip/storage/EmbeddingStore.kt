package com.mira.clip.storage

import com.mira.clip.core.Config
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * File-based embedding store for CLIP4Clip.
 * 
 * Persists embeddings as .f32 (float32 LE) and JSON metadata.
 * Easy to diff, hash, and ship as portable artifacts.
 */
interface EmbeddingStore {
    
    /**
     * Store embedding vector and metadata.
     * 
     * @param variant CLIP variant name
     * @param videoId Video identifier
     * @param embedding Embedding vector
     * @param metadata Additional metadata
     */
    fun storeEmbedding(variant: String, videoId: String, embedding: FloatArray, metadata: Map<String, Any>)
    
    /**
     * Load embedding vector.
     * 
     * @param variant CLIP variant name
     * @param videoId Video identifier
     * @return Embedding vector or null if not found
     */
    fun loadEmbedding(variant: String, videoId: String): FloatArray?
    
    /**
     * Load embedding metadata.
     * 
     * @param variant CLIP variant name
     * @param videoId Video identifier
     * @return Metadata map or null if not found
     */
    fun loadMetadata(variant: String, videoId: String): Map<String, Any>?
    
    /**
     * List all stored video IDs for a variant.
     * 
     * @param variant CLIP variant name
     * @return List of video IDs
     */
    fun listVideoIds(variant: String): List<String>
    
    /**
     * Check if embedding exists.
     * 
     * @param variant CLIP variant name
     * @param videoId Video identifier
     * @return true if embedding exists
     */
    fun hasEmbedding(variant: String, videoId: String): Boolean
}

/**
 * File-based implementation of EmbeddingStore.
 * 
 * Stores vectors as: ${OUT_ROOT}/embeddings/{variant}/{videoId}.f32
 * Stores metadata as: ${OUT_ROOT}/embeddings/{variant}/{videoId}.json
 */
class FileEmbeddingStore : EmbeddingStore {
    
    private val embeddingsDir = File(Config.EMBEDDINGS_DIR)
    
    init {
        // Ensure embeddings directory exists
        embeddingsDir.mkdirs()
    }
    
    override fun storeEmbedding(variant: String, videoId: String, embedding: FloatArray, metadata: Map<String, Any>) {
        val variantDir = File(embeddingsDir, variant)
        variantDir.mkdirs()
        
        // Store vector as .f32 file
        val vectorFile = File(variantDir, "$videoId.f32")
        storeVector(vectorFile, embedding)
        
        // Store metadata as .json file
        val metadataFile = File(variantDir, "$videoId.json")
        storeMetadata(metadataFile, metadata)
    }
    
    override fun loadEmbedding(variant: String, videoId: String): FloatArray? {
        val vectorFile = File(File(embeddingsDir, variant), "$videoId.f32")
        return if (vectorFile.exists()) {
            loadVector(vectorFile)
        } else {
            null
        }
    }
    
    override fun loadMetadata(variant: String, videoId: String): Map<String, Any>? {
        val metadataFile = File(File(embeddingsDir, variant), "$videoId.json")
        return if (metadataFile.exists()) {
            loadMetadata(metadataFile)
        } else {
            null
        }
    }
    
    override fun listVideoIds(variant: String): List<String> {
        val variantDir = File(embeddingsDir, variant)
        if (!variantDir.exists()) {
            return emptyList()
        }
        
        return variantDir.listFiles { file ->
            file.isFile && file.name.endsWith(".f32")
        }?.map { file ->
            file.nameWithoutExtension
        } ?: emptyList()
    }
    
    override fun hasEmbedding(variant: String, videoId: String): Boolean {
        val vectorFile = File(File(embeddingsDir, variant), "$videoId.f32")
        return vectorFile.exists()
    }
    
    /**
     * Store embedding vector as float32 little-endian binary file.
     */
    private fun storeVector(file: File, embedding: FloatArray) {
        val buffer = ByteBuffer.allocate(embedding.size * 4)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        
        for (value in embedding) {
            buffer.putFloat(value)
        }
        
        file.writeBytes(buffer.array())
    }
    
    /**
     * Load embedding vector from float32 little-endian binary file.
     */
    private fun loadVector(file: File): FloatArray {
        val bytes = file.readBytes()
        val buffer = ByteBuffer.wrap(bytes)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        
        val embedding = FloatArray(bytes.size / 4)
        for (i in embedding.indices) {
            embedding[i] = buffer.float
        }
        
        return embedding
    }
    
    /**
     * Store metadata as JSON file.
     */
    private fun storeMetadata(file: File, metadata: Map<String, Any>) {
        val json = JSONObject()
        for ((key, value) in metadata) {
            json.put(key, value)
        }
        
        file.writeText(json.toString(2))
    }
    
    /**
     * Load metadata from JSON file.
     */
    private fun loadMetadata(file: File): Map<String, Any> {
        val json = JSONObject(file.readText())
        val metadata = mutableMapOf<String, Any>()
        
        for (key in json.keys()) {
            metadata[key] = json.get(key)
        }
        
        return metadata
    }
}
