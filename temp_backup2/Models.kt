package com.mira.videoeditor.db

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Read models with relations for efficient data loading.
 * 
 * These models combine entities with their related data using Room's @Relation
 * annotation for efficient joins and object graph loading.
 */

data class VideoWithEmbedding(
    @Embedded val video: VideoEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "ownerId",
        entity = EmbeddingEntity::class
    )
    val embeddings: List<EmbeddingEntity> // filter by variant in DAO queries
)

data class ShotWithEmbedding(
    @Embedded val shot: ShotEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "ownerId",
        entity = EmbeddingEntity::class
    )
    val embeddings: List<EmbeddingEntity>
)

data class VideoWithShots(
    @Embedded val video: VideoEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "videoId"
    )
    val shots: List<ShotEntity>
)

data class VideoWithShotsAndEmbeddings(
    @Embedded val video: VideoEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "videoId"
    )
    val shots: List<ShotEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "ownerId",
        entity = EmbeddingEntity::class
    )
    val embeddings: List<EmbeddingEntity>
)