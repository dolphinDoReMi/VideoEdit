package com.mira.videoeditor.db

import androidx.room.*
import java.util.*

/**
 * Room entities for CLIP4Clip video-text retrieval system.
 * 
 * This file contains all database entities for storing:
 * - Video metadata and URIs
 * - Shot boundaries and keyframes
 * - Text queries and embeddings
 * - Vector embeddings for videos, shots, and text
 */

@Entity(
    tableName = "videos",
    indices = [Index("createdAt")]
)
data class VideoEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val uri: String,                 // content:// or file://
    val durationMs: Long,
    val fps: Float,
    val width: Int,
    val height: Int,
    val metadataJson: String = "{}", // exif, labels, etc.
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "shots",
    indices = [Index("videoId"), Index("startMs"), Index("endMs")]
)
data class ShotEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val videoId: String,
    val startMs: Long,
    val endMs: Long,
    val keyframePath: String? = null // local file for preview / caching
)

@Entity(
    tableName = "texts",
    indices = [Index("createdAt")]
)
data class TextEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val text: String,
    val lang: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Embedding storage for CLIP4Clip vectors.
 * 
 * @param ownerType "video" | "shot" | "text"
 * @param variant e.g. "clip_vit_b32_mean_v1" or "clip_vit_b32_txtr_v1"
 * @param vec little-endian float32[dim] (normalized)
 */
@Entity(
    tableName = "embeddings",
    indices = [
        Index("ownerType"), 
        Index("ownerId"), 
        Index("variant"),
        Index(value = ["ownerType", "ownerId", "variant"], unique = true)
    ]
)
data class EmbeddingEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ownerType: String,          // "video" | "shot" | "text"
    val ownerId: String,             // videoId | shotId | textId
    val dim: Int,                    // e.g., 512 or 768
    val variant: String,             // "clip_vit_b32_mean_v1"
    val vec: ByteArray,              // little-endian float32 blob (dim * 4 bytes)
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EmbeddingEntity

        if (id != other.id) return false
        if (ownerType != other.ownerType) return false
        if (ownerId != other.ownerId) return false
        if (dim != other.dim) return false
        if (variant != other.variant) return false
        if (!vec.contentEquals(other.vec)) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + ownerType.hashCode()
        result = 31 * result + ownerId.hashCode()
        result = 31 * result + dim
        result = 31 * result + variant.hashCode()
        result = 31 * result + vec.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}

/**
 * Optional: Full-Text Search for texts
 * This gives you quick lexical search to complement vector search.
 */
@Fts4(contentEntity = TextEntity::class)
@Entity(tableName = "texts_fts")
data class TextFts(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Int,
    val text: String
)