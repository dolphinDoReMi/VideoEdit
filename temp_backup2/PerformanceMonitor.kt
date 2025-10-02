package com.mira.videoeditor.data

import android.os.SystemClock
import androidx.tracing.trace
import com.mira.videoeditor.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance monitoring and caching system for CLIP4Clip operations.
 * 
 * Tracks performance metrics, provides in-memory vector caching,
 * and monitors resource usage for optimization.
 */
@Singleton
class PerformanceMonitor @Inject constructor() {
    
    companion object {
        private const val MAX_CACHE_SIZE = 10000 // Maximum vectors to cache
        private const val CACHE_TTL_MS = 300000L // 5 minutes TTL
    }

    // Performance metrics
    private val metrics = ConcurrentHashMap<String, PerformanceMetric>()
    private val cacheMutex = Mutex()
    
    // In-memory vector cache
    private val vectorCache = ConcurrentHashMap<String, CachedVector>()
    
    /**
     * Track embedding generation performance.
     */
    fun trackEmbeddingGeneration(
        operation: String,
        vectorCount: Int,
        durationMs: Long,
        variant: String
    ) {
        val key = "embedding_${operation}_${variant}"
        metrics[key] = PerformanceMetric(
            operation = operation,
            count = vectorCount,
            totalDurationMs = durationMs,
            averageDurationMs = durationMs / vectorCount.coerceAtLeast(1),
            variant = variant,
            timestamp = System.currentTimeMillis()
        )
        
        Logger.info(Logger.Category.PERFORMANCE, "Embedding generation tracked", mapOf(
            "operation" to operation,
            "vectorCount" to vectorCount,
            "durationMs" to durationMs,
            "variant" to variant,
            "avgMsPerVector" to (durationMs / vectorCount.coerceAtLeast(1))
        ))
    }

    /**
     * Track search performance.
     */
    fun trackSearchPerformance(
        query: String,
        resultCount: Int,
        durationMs: Long,
        variant: String,
        searchLevel: String
    ) {
        val key = "search_${searchLevel}_${variant}"
        metrics[key] = PerformanceMetric(
            operation = "search_$searchLevel",
            count = resultCount,
            totalDurationMs = durationMs,
            averageDurationMs = durationMs,
            variant = variant,
            timestamp = System.currentTimeMillis()
        )
        
        Logger.info(Logger.Category.PERFORMANCE, "Search performance tracked", mapOf(
            "query" to query.take(50),
            "resultCount" to resultCount,
            "durationMs" to durationMs,
            "variant" to variant,
            "searchLevel" to searchLevel
        ))
    }

    /**
     * Track database operation performance.
     */
    fun trackDatabaseOperation(
        operation: String,
        recordCount: Int,
        durationMs: Long
    ) {
        val key = "db_$operation"
        metrics[key] = PerformanceMetric(
            operation = operation,
            count = recordCount,
            totalDurationMs = durationMs,
            averageDurationMs = durationMs,
            variant = "database",
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Cache a vector for faster retrieval.
     */
    suspend fun cacheVector(
        ownerId: String,
        variant: String,
        vector: FloatArray
    ) = cacheMutex.withLock {
        if (vectorCache.size >= MAX_CACHE_SIZE) {
            // Remove oldest entries
            val oldestKey = vectorCache.minByOrNull { it.value.timestamp }?.key
            oldestKey?.let { vectorCache.remove(it) }
        }
        
        val key = "${ownerId}_$variant"
        vectorCache[key] = CachedVector(
            vector = vector.copyOf(),
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Retrieve a cached vector.
     */
    suspend fun getCachedVector(
        ownerId: String,
        variant: String
    ): FloatArray? = cacheMutex.withLock {
        val key = "${ownerId}_$variant"
        val cached = vectorCache[key]
        
        if (cached != null) {
            val age = System.currentTimeMillis() - cached.timestamp
            if (age <= CACHE_TTL_MS) {
                return@withLock cached.vector.copyOf()
            } else {
                // Remove expired cache entry
                vectorCache.remove(key)
            }
        }
        
        null
    }

    /**
     * Clear the vector cache.
     */
    suspend fun clearCache() = cacheMutex.withLock {
        vectorCache.clear()
    }

    /**
     * Get cache statistics.
     */
    suspend fun getCacheStats(): CacheStats = cacheMutex.withLock {
        val now = System.currentTimeMillis()
        val validEntries = vectorCache.values.count { 
            now - it.timestamp <= CACHE_TTL_MS 
        }
        
        CacheStats(
            totalEntries = vectorCache.size,
            validEntries = validEntries,
            expiredEntries = vectorCache.size - validEntries,
            maxSize = MAX_CACHE_SIZE,
            ttlMs = CACHE_TTL_MS
        )
    }

    /**
     * Get performance metrics summary.
     */
    fun getPerformanceSummary(): Map<String, PerformanceMetric> {
        return metrics.toMap()
    }

    /**
     * Clear all performance metrics.
     */
    fun clearMetrics() {
        metrics.clear()
    }

    /**
     * Execute a traced operation for performance monitoring.
     */
    suspend fun <T> traceOperation(
        operationName: String,
        block: suspend () -> T
    ): T {
        return trace(operationName) {
            val startTime = SystemClock.elapsedRealtime()
            try {
                block()
            } finally {
                val duration = SystemClock.elapsedRealtime() - startTime
                trackDatabaseOperation(operationName, 1, duration)
            }
        }
    }
}

/**
 * Performance metric data class.
 */
data class PerformanceMetric(
    val operation: String,
    val count: Int,
    val totalDurationMs: Long,
    val averageDurationMs: Long,
    val variant: String,
    val timestamp: Long
)

/**
 * Cached vector data class.
 */
data class CachedVector(
    val vector: FloatArray,
    val timestamp: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CachedVector

        if (!vector.contentEquals(other.vector)) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vector.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * Cache statistics data class.
 */
data class CacheStats(
    val totalEntries: Int,
    val validEntries: Int,
    val expiredEntries: Int,
    val maxSize: Int,
    val ttlMs: Long
)
