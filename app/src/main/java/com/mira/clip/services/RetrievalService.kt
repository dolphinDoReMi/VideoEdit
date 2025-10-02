package com.mira.clip.services

import android.content.Context
import com.mira.clip.clip.ClipEngines
import com.mira.clip.storage.EmbeddingStore
import com.mira.clip.storage.FileEmbeddingStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.sqrt

/**
 * Retrieval service for CLIP4Clip text-to-video search.
 * 
 * Performs text queries against stored video embeddings.
 */
class RetrievalService(private val context: Context) {
    
    private val clipEngines = ClipEngines(context)
    private val embeddingStore: EmbeddingStore = FileEmbeddingStore()
    
    /**
     * Run text-to-video retrieval from manifest file.
     * 
     * @param manifestPath Path to search manifest JSON
     */
    fun run(manifestPath: String) {
        val manifest = loadManifest(manifestPath)
        
        // Initialize CLIP engines
        clipEngines.initialize()
        
        val variant = manifest.getString("variant")
        val queries = manifest.getJSONArray("queries")
        val topK = manifest.optInt("top_k", 5)
        val indexDir = manifest.getString("index_dir")
        val outputPath = manifest.getString("output_path")
        
        // Load all video embeddings for this variant
        val videoEmbeddings = loadVideoEmbeddings(variant)
        
        val results = JSONObject()
        val queryResults = JSONArray()
        
        for (i in 0 until queries.length()) {
            val query = queries.getJSONObject(i)
            val queryId = query.getString("id")
            val queryText = query.getString("text")
            
            println("Processing query: $queryId - $queryText")
            
            // Encode query text
            val queryEmbedding = clipEngines.encodeText(queryText)
            
            // Compute similarities
            val similarities = computeSimilarities(queryEmbedding, videoEmbeddings)
            
            // Get top-k results
            val topResults = similarities
                .sortedByDescending { it.second }
                .take(topK)
            
            // Create result object
            val queryResult = JSONObject()
            queryResult.put("query_id", queryId)
            queryResult.put("query_text", queryText)
            
            val topKArray = JSONArray()
            for ((videoId, score) in topResults) {
                val result = JSONObject()
                result.put("video_id", videoId)
                result.put("score", score)
                topKArray.put(result)
            }
            
            queryResult.put("top_k", topKArray)
            queryResults.put(queryResult)
            
            println("Query $queryId completed with ${topResults.size} results")
        }
        
        results.put("results", queryResults)
        
        // Save results
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(results.toString(2))
        
        println("Search results saved to: $outputPath")
    }
    
    /**
     * Load all video embeddings for a variant.
     */
    private fun loadVideoEmbeddings(variant: String): Map<String, FloatArray> {
        val videoIds = embeddingStore.listVideoIds(variant)
        val embeddings = mutableMapOf<String, FloatArray>()
        
        for (videoId in videoIds) {
            val embedding = embeddingStore.loadEmbedding(variant, videoId)
            if (embedding != null) {
                embeddings[videoId] = embedding
            }
        }
        
        println("Loaded ${embeddings.size} video embeddings for variant: $variant")
        return embeddings
    }
    
    /**
     * Compute cosine similarities between query and video embeddings.
     */
    private fun computeSimilarities(queryEmbedding: FloatArray, videoEmbeddings: Map<String, FloatArray>): List<Pair<String, Float>> {
        val similarities = mutableListOf<Pair<String, Float>>()
        
        for ((videoId, videoEmbedding) in videoEmbeddings) {
            val similarity = computeCosineSimilarity(queryEmbedding, videoEmbedding)
            similarities.add(Pair(videoId, similarity))
        }
        
        return similarities
    }
    
    /**
     * Compute cosine similarity between two normalized vectors.
     */
    private fun computeCosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) {
            throw IllegalArgumentException("Embedding dimensions must match")
        }
        
        var dotProduct = 0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
        }
        
        // Since vectors are L2-normalized, cosine similarity = dot product
        return dotProduct
    }
    
    /**
     * Load search manifest from JSON file.
     */
    private fun loadManifest(path: String): JSONObject {
        val file = File(path)
        if (!file.exists()) {
            throw IllegalArgumentException("Manifest file does not exist: $path")
        }
        
        return JSONObject(file.readText())
    }
}
