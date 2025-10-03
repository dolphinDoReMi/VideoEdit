package com.mira.clip.index.faiss

import android.content.Context

enum class FaissIndexType { FLAT_IP, IVF_PQ, HNSW_IP }

data class FaissDesignConfig(
    val indexRootSubpath: String = "MiraClip/out/faiss",
    val metric: String = "ip",                 // "ip" (cosine needs normalized inputs) or "l2"
    val indexType: FaissIndexType = FaissIndexType.IVF_PQ,
    val dim: Int = 512,
    // IVF+PQ
    val nlist: Int = 4096,
    val nprobe: Int = 16,
    val pqM: Int = 64,
    val pqBits: Int = 8,
    // HNSW
    val hnswM: Int = 32,
    val efConstruction: Int = 200,
    val efSearch: Int = 64,
    // Segmenting / compaction
    val segmentTargetN: Int = 512,             // vectors per segment
    val compactionEnabled: Boolean = false,
    val compactionMinSegments: Int = 16,
    // IDs
    val idHashSalt: Long = 0x7F4A7C15L,
    // Publishing
    val atomicPublish: Boolean = true,
    val schemaVersion: Int = 1,
    val variant: String = "base"
)

object FaissConfig {
    @Volatile private var active = FaissDesignConfig()
    fun get(): FaissDesignConfig = active
    fun update(cfg: FaissDesignConfig) { active = cfg }

    fun resolveRoot(context: Context) =
        java.io.File(context.getExternalFilesDir(null), get().indexRootSubpath)
}
