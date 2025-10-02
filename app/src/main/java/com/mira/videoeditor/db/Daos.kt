package com.mira.videoeditor.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Objects (DAOs) for Room database operations.
 * 
 * These interfaces define all database operations for the CLIP4Clip system,
 * including CRUD operations and specialized queries for video-text retrieval.
 */

@Dao
interface VideoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(video: VideoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(videos: List<VideoEntity>)

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getById(id: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE uri = :uri")
    suspend fun getByUri(uri: String): VideoEntity?

    @Query("SELECT * FROM videos ORDER BY createdAt DESC")
    fun streamAll(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getLatest(limit: Int): List<VideoEntity>

    @Delete
    suspend fun delete(video: VideoEntity)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ShotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(shot: ShotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(shots: List<ShotEntity>)

    @Query("SELECT * FROM shots WHERE id = :id")
    suspend fun getById(id: String): ShotEntity?

    @Query("SELECT * FROM shots WHERE videoId = :videoId ORDER BY startMs")
    suspend fun forVideo(videoId: String): List<ShotEntity>

    @Query("SELECT * FROM shots WHERE videoId = :videoId AND startMs >= :startMs AND endMs <= :endMs ORDER BY startMs")
    suspend fun forVideoInRange(videoId: String, startMs: Long, endMs: Long): List<ShotEntity>

    @Delete
    suspend fun delete(shot: ShotEntity)

    @Query("DELETE FROM shots WHERE videoId = :videoId")
    suspend fun deleteByVideoId(videoId: String)
}

@Dao
interface TextDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(text: TextEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(texts: List<TextEntity>)

    @Query("SELECT * FROM texts WHERE id = :id")
    suspend fun getById(id: String): TextEntity?

    @Query("SELECT * FROM texts ORDER BY createdAt DESC LIMIT :limit")
    suspend fun latest(limit: Int): List<TextEntity>

    @Query("SELECT * FROM texts WHERE text LIKE :query ORDER BY createdAt DESC LIMIT :limit")
    suspend fun searchByText(query: String, limit: Int = 20): List<TextEntity>

    @Delete
    suspend fun delete(text: TextEntity)
}

@Dao
interface EmbeddingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: EmbeddingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(e: List<EmbeddingEntity>)

    @Query("SELECT * FROM embeddings WHERE id = :id")
    suspend fun getById(id: String): EmbeddingEntity?

    @Query("SELECT * FROM embeddings WHERE ownerId = :ownerId")
    suspend fun forOwner(ownerId: String): List<EmbeddingEntity>

    @Query("SELECT * FROM embeddings WHERE ownerId = :ownerId AND variant = :variant")
    suspend fun forOwnerAndVariant(ownerId: String, variant: String): EmbeddingEntity?

    @Query("SELECT * FROM embeddings WHERE ownerType = :ownerType AND variant = :variant")
    suspend fun listByTypeAndVariant(ownerType: String, variant: String): List<EmbeddingEntity>

    @Query("SELECT * FROM embeddings WHERE ownerType = 'video' AND variant = :variant")
    suspend fun listAllVideoEmbeddings(variant: String): List<EmbeddingEntity>

    @Query("SELECT * FROM embeddings WHERE ownerType = 'shot' AND variant = :variant")
    suspend fun listAllShotEmbeddings(variant: String): List<EmbeddingEntity>

    @Query("SELECT * FROM embeddings WHERE ownerType = 'text' AND variant = :variant")
    suspend fun listAllTextEmbeddings(variant: String): List<EmbeddingEntity>

    @Query("SELECT COUNT(*) FROM embeddings WHERE variant = :variant")
    suspend fun countByVariant(variant: String): Int

    @Delete
    suspend fun delete(e: EmbeddingEntity)

    @Query("DELETE FROM embeddings WHERE ownerId = :ownerId")
    suspend fun deleteByOwner(ownerId: String)

    @Query("DELETE FROM embeddings WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun deleteByOwnerTypeAndId(ownerType: String, ownerId: String)
}

@Dao
interface ReadModelsDao {
    @Transaction
    @Query("""
        SELECT * FROM videos WHERE id IN 
        (SELECT ownerId FROM embeddings WHERE ownerType='video' AND variant=:variant)
    """)
    suspend fun videosWithEmbeddings(variant: String): List<VideoWithEmbedding>

    @Transaction
    @Query("""
        SELECT * FROM shots WHERE videoId = :videoId AND id IN 
        (SELECT ownerId FROM embeddings WHERE ownerType='shot' AND variant=:variant)
    """)
    suspend fun shotsWithEmbeddings(videoId: String, variant: String): List<ShotWithEmbedding>

    @Transaction
    @Query("SELECT * FROM videos WHERE id = :videoId")
    suspend fun videoWithShots(videoId: String): VideoWithShots?

    @Transaction
    @Query("SELECT * FROM videos WHERE id = :videoId")
    suspend fun videoWithShotsAndEmbeddings(videoId: String): VideoWithShotsAndEmbeddings?
}

@Dao
interface TextFtsDao {
    @Query("""
        SELECT t.* FROM texts t 
        JOIN texts_fts fts ON t.rowid = fts.rowid
        WHERE texts_fts MATCH :query
        ORDER BY rank
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int = 20): List<TextEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(fts: TextFts)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(fts: List<TextFts>)
}