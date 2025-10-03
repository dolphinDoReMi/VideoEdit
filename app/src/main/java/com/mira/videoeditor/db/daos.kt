package com.mira.videoeditor.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Video operations
 */
@Dao
interface VideoDao {
    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getById(id: String): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(video: VideoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(videos: List<VideoEntity>)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM videos")
    suspend fun getAll(): List<VideoEntity>
}

/**
 * Data Access Object for Shot operations
 */
@Dao
interface ShotDao {
    @Query("SELECT * FROM shots WHERE videoId = :videoId")
    suspend fun forVideo(videoId: String): List<ShotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(shot: ShotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(shots: List<ShotEntity>)

    @Query("DELETE FROM shots WHERE videoId = :videoId")
    suspend fun deleteByVideoId(videoId: String)

    @Query("SELECT * FROM shots")
    suspend fun getAll(): List<ShotEntity>
}

/**
 * Data Access Object for Text operations
 */
@Dao
interface TextDao {
    @Query("SELECT * FROM texts WHERE id = :id")
    suspend fun getById(id: String): TextEntity?

    @Query("SELECT * FROM texts WHERE text LIKE '%' || :query || '%'")
    suspend fun searchByText(query: String): List<TextEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(text: TextEntity)

    @Delete
    suspend fun delete(text: TextEntity)

    @Query("SELECT * FROM texts")
    suspend fun getAll(): List<TextEntity>
}

/**
 * Data Access Object for Embedding operations
 */
@Dao
interface EmbeddingDao {
    @Query("SELECT * FROM embeddings WHERE ownerType = :ownerType AND ownerId = :ownerId AND variant = :variant")
    suspend fun forOwnerAndVariant(ownerType: String, ownerId: String, variant: String): List<EmbeddingEntity>

    @Query("SELECT * FROM embeddings WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun forOwner(ownerType: String, ownerId: String): List<EmbeddingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(embedding: EmbeddingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(embeddings: List<EmbeddingEntity>)

    @Query("DELETE FROM embeddings WHERE ownerType = :ownerType AND ownerId = :ownerId AND variant = :variant")
    suspend fun deleteByOwnerTypeAndId(ownerType: String, ownerId: String, variant: String)

    @Query("SELECT COUNT(*) FROM embeddings WHERE variant = :variant")
    suspend fun countByVariant(variant: String): Int

    @Query("SELECT * FROM embeddings WHERE variant = :variant")
    suspend fun listByTypeAndVariant(variant: String): List<EmbeddingEntity>

    @Query("SELECT * FROM embeddings")
    suspend fun listAllVideoEmbeddings(): List<EmbeddingEntity>

    @Query("SELECT * FROM embeddings WHERE ownerType = 'shot'")
    suspend fun listAllShotEmbeddings(): List<EmbeddingEntity>
}

/**
 * Data Access Object for Read Models operations
 */
@Dao
interface ReadModelsDao {
    @Query("SELECT * FROM read_models WHERE videoId = :videoId ORDER BY processedAt DESC LIMIT 1")
    suspend fun getLatest(videoId: String): ReadModelsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(readModel: ReadModelsEntity)

    @Query("SELECT * FROM read_models")
    suspend fun getAll(): List<ReadModelsEntity>
}
