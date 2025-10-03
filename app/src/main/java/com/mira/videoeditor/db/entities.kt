package com.mira.videoeditor.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

/**
 * Video entity representing a video file in the database
 */
@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val uri: String,
    val title: String,
    val duration: Long,
    val size: Long,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Shot entity representing a video segment/clip
 */
@Entity(tableName = "shots")
data class ShotEntity(
    @PrimaryKey val id: String,
    val videoId: String,
    val startMs: Long,
    val endMs: Long,
    val score: Float = 0f,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Text entity for storing transcribed text
 */
@Entity(tableName = "texts")
data class TextEntity(
    @PrimaryKey val id: String,
    val videoId: String,
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Embedding entity for storing CLIP embeddings
 */
@Entity(tableName = "embeddings")
data class EmbeddingEntity(
    @PrimaryKey val id: String,
    val ownerType: String, // "video" or "shot"
    val ownerId: String,
    val variant: String, // "image" or "text"
    val dim: Int,
    val frameCount: Int = 1,
    @TypeConverters(VectorConverters::class)
    val vec: FloatArray,
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EmbeddingEntity

        if (id != other.id) return false
        if (ownerType != other.ownerType) return false
        if (ownerId != other.ownerId) return false
        if (variant != other.variant) return false
        if (dim != other.dim) return false
        if (frameCount != other.frameCount) return false
        if (!vec.contentEquals(other.vec)) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + ownerType.hashCode()
        result = 31 * result + ownerId.hashCode()
        result = 31 * result + variant.hashCode()
        result = 31 * result + dim
        result = 31 * result + frameCount
        result = 31 * result + vec.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}

/**
 * Read models entity for tracking processed models
 */
@Entity(tableName = "read_models")
data class ReadModelsEntity(
    @PrimaryKey val id: String,
    val videoId: String,
    val modelVersion: String,
    val processedAt: Long = System.currentTimeMillis()
)
