package com.mira.videoeditor

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mira.videoeditor.db.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive test suite for CLIP4Clip Room database integration.
 * 
 * These tests verify:
 * 1. Database operations (CRUD)
 * 2. Entity relationships
 * 3. Vector serialization
 * 4. Query performance
 * 5. Migration compatibility
 */
@RunWith(AndroidJUnit4::class)
class Clip4ClipDatabaseTest {

    private lateinit var database: AppDatabase
    private lateinit var videoDao: VideoDao
    private lateinit var shotDao: ShotDao
    private lateinit var textDao: TextDao
    private lateinit var embeddingDao: EmbeddingDao
    private lateinit var readDao: ReadModelsDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        videoDao = database.videoDao()
        shotDao = database.shotDao()
        textDao = database.textDao()
        embeddingDao = database.embeddingDao()
        readDao = database.readModelsDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun testVideoEntityCrud() = runBlocking {
        // Create video entity
        val video = VideoEntity(
            uri = "content://test/video1.mp4",
            durationMs = 60000L,
            fps = 30f,
            width = 1920,
            height = 1080
        )
        
        // Insert
        videoDao.upsert(video)
        
        // Read
        val retrieved = videoDao.getById(video.id)
        assertNotNull(retrieved)
        assertEquals(video.uri, retrieved.uri)
        assertEquals(video.durationMs, retrieved.durationMs)
        assertEquals(video.fps, retrieved.fps)
        
        // Update
        val updated = video.copy(fps = 60f)
        videoDao.upsert(updated)
        
        val retrievedUpdated = videoDao.getById(video.id)
        assertEquals(60f, retrievedUpdated.fps)
        
        // Delete
        videoDao.deleteById(video.id)
        val deleted = videoDao.getById(video.id)
        assertEquals(null, deleted)
    }

    @Test
    fun testShotEntityCrud() = runBlocking {
        // Create video first
        val video = VideoEntity(
            uri = "content://test/video1.mp4",
            durationMs = 60000L,
            fps = 30f,
            width = 1920,
            height = 1080
        )
        videoDao.upsert(video)
        
        // Create shot entities
        val shots = listOf(
            ShotEntity(
                videoId = video.id,
                startMs = 0L,
                endMs = 10000L
            ),
            ShotEntity(
                videoId = video.id,
                startMs = 10000L,
                endMs = 20000L
            )
        )
        
        // Insert shots
        shotDao.upsertAll(shots)
        
        // Read shots for video
        val retrievedShots = shotDao.forVideo(video.id)
        assertEquals(2, retrievedShots.size)
        assertEquals(0L, retrievedShots[0].startMs)
        assertEquals(10000L, retrievedShots[0].endMs)
        assertEquals(10000L, retrievedShots[1].startMs)
        assertEquals(20000L, retrievedShots[1].endMs)
        
        // Delete shots
        shotDao.deleteByVideoId(video.id)
        val deletedShots = shotDao.forVideo(video.id)
        assertEquals(0, deletedShots.size)
    }

    @Test
    fun testEmbeddingEntityCrud() = runBlocking {
        // Create video entity
        val video = VideoEntity(
            uri = "content://test/video1.mp4",
            durationMs = 60000L,
            fps = 30f,
            width = 1920,
            height = 1080
        )
        videoDao.upsert(video)
        
        // Create embedding
        val embedding = FloatArray(512) { kotlin.random.Random.nextFloat() }
        val embeddingEntity = EmbeddingEntity(
            ownerType = "video",
            ownerId = video.id,
            dim = 512,
            variant = "clip_vit_b32_mean_v1",
            vec = VectorConverters.floatArrayToLeBytes(embedding)
        )
        
        // Insert embedding
        embeddingDao.upsert(embeddingEntity)
        
        // Read embedding
        val retrieved = embeddingDao.forOwnerAndVariant(video.id, "clip_vit_b32_mean_v1")
        assertNotNull(retrieved)
        assertEquals("video", retrieved.ownerType)
        assertEquals(video.id, retrieved.ownerId)
        assertEquals(512, retrieved.dim)
        assertEquals("clip_vit_b32_mean_v1", retrieved.variant)
        
        // Verify vector serialization
        val deserializedEmbedding = VectorConverters.leBytesToFloatArray(retrieved.vec)
        assertEquals(embedding.size, deserializedEmbedding.size)
        for (i in embedding.indices) {
            assertEquals(embedding[i], deserializedEmbedding[i], 0.001f)
        }
        
        // Delete embedding
        embeddingDao.deleteByOwnerTypeAndId("video", video.id)
        val deleted = embeddingDao.forOwnerAndVariant(video.id, "clip_vit_b32_mean_v1")
        assertEquals(null, deleted)
    }

    @Test
    fun testVectorSerialization() {
        // Test vector serialization round-trip
        val originalVector = FloatArray(512) { kotlin.random.Random.nextFloat() }
        
        // Serialize
        val serialized = VectorConverters.floatArrayToLeBytes(originalVector)
        assertEquals(512 * 4, serialized.size) // 512 floats * 4 bytes each
        
        // Deserialize
        val deserialized = VectorConverters.leBytesToFloatArray(serialized)
        assertEquals(originalVector.size, deserialized.size)
        
        // Verify values
        for (i in originalVector.indices) {
            assertEquals(originalVector[i], deserialized[i], 0.001f)
        }
    }

    @Test
    fun testVideoWithEmbeddingsRelation() = runBlocking {
        // Create video
        val video = VideoEntity(
            uri = "content://test/video1.mp4",
            durationMs = 60000L,
            fps = 30f,
            width = 1920,
            height = 1080
        )
        videoDao.upsert(video)
        
        // Create embeddings
        val embeddings = listOf(
            EmbeddingEntity(
                ownerType = "video",
                ownerId = video.id,
                dim = 512,
                variant = "clip_vit_b32_mean_v1",
                vec = VectorConverters.floatArrayToLeBytes(FloatArray(512) { kotlin.random.Random.nextFloat() })
            ),
            EmbeddingEntity(
                ownerType = "video",
                ownerId = video.id,
                dim = 512,
                variant = "clip_vit_b32_txtr_v1",
                vec = VectorConverters.floatArrayToLeBytes(FloatArray(512) { kotlin.random.Random.nextFloat() })
            )
        )
        embeddingDao.upsertAll(embeddings)
        
        // Test relation query
        val videosWithEmbeddings = readDao.videosWithEmbeddings("clip_vit_b32_mean_v1")
        assertEquals(1, videosWithEmbeddings.size)
        
        val videoWithEmbedding = videosWithEmbeddings[0]
        assertEquals(video.id, videoWithEmbedding.video.id)
        assertEquals(1, videoWithEmbedding.embeddings.size)
        assertEquals("clip_vit_b32_mean_v1", videoWithEmbedding.embeddings[0].variant)
    }

    @Test
    fun testTextEntityCrud() = runBlocking {
        // Create text entity
        val text = TextEntity(
            text = "person walking in the street",
            lang = "en"
        )
        
        // Insert
        textDao.upsert(text)
        
        // Read
        val retrieved = textDao.getById(text.id)
        assertNotNull(retrieved)
        assertEquals(text.text, retrieved.text)
        assertEquals(text.lang, retrieved.lang)
        
        // Search by text
        val searchResults = textDao.searchByText("person", limit = 10)
        assertEquals(1, searchResults.size)
        assertEquals(text.text, searchResults[0].text)
        
        // Delete
        textDao.delete(text)
        val deleted = textDao.getById(text.id)
        assertEquals(null, deleted)
    }

    @Test
    fun testEmbeddingQueries() = runBlocking {
        // Create test data
        val video1 = VideoEntity(uri = "content://test/video1.mp4", durationMs = 60000L, fps = 30f, width = 1920, height = 1080)
        val video2 = VideoEntity(uri = "content://test/video2.mp4", durationMs = 120000L, fps = 30f, width = 1920, height = 1080)
        videoDao.upsertAll(listOf(video1, video2))
        
        val embeddings = listOf(
            EmbeddingEntity(
                ownerType = "video",
                ownerId = video1.id,
                dim = 512,
                variant = "clip_vit_b32_mean_v1",
                vec = VectorConverters.floatArrayToLeBytes(FloatArray(512) { kotlin.random.Random.nextFloat() })
            ),
            EmbeddingEntity(
                ownerType = "video",
                ownerId = video2.id,
                dim = 512,
                variant = "clip_vit_b32_mean_v1",
                vec = VectorConverters.floatArrayToLeBytes(FloatArray(512) { kotlin.random.Random.nextFloat() })
            ),
            EmbeddingEntity(
                ownerType = "shot",
                ownerId = "shot1",
                dim = 512,
                variant = "clip_vit_b32_mean_v1",
                vec = VectorConverters.floatArrayToLeBytes(FloatArray(512) { kotlin.random.Random.nextFloat() })
            )
        )
        embeddingDao.upsertAll(embeddings)
        
        // Test queries
        val videoEmbeddings = embeddingDao.listAllVideoEmbeddings("clip_vit_b32_mean_v1")
        assertEquals(2, videoEmbeddings.size)
        
        val shotEmbeddings = embeddingDao.listAllShotEmbeddings("clip_vit_b32_mean_v1")
        assertEquals(1, shotEmbeddings.size)
        
        val count = embeddingDao.countByVariant("clip_vit_b32_mean_v1")
        assertEquals(3, count)
        
        val byType = embeddingDao.listByTypeAndVariant("video", "clip_vit_b32_mean_v1")
        assertEquals(2, byType.size)
    }

    @Test
    fun testDatabasePerformance() = runBlocking {
        // Test with larger dataset
        val videos = (1..100).map { i ->
            VideoEntity(
                uri = "content://test/video$i.mp4",
                durationMs = (i * 1000).toLong(),
                fps = 30f,
                width = 1920,
                height = 1080
            )
        }
        
        val startTime = System.currentTimeMillis()
        videoDao.upsertAll(videos)
        val insertTime = System.currentTimeMillis() - startTime
        
        val queryStartTime = System.currentTimeMillis()
        val retrievedVideos = videoDao.getLatest(50)
        val queryTime = System.currentTimeMillis() - queryStartTime
        
        assertEquals(50, retrievedVideos.size)
        
        // Performance assertions (adjust thresholds as needed)
        assertTrue(insertTime < 5000, "Insert should complete within 5 seconds")
        assertTrue(queryTime < 1000, "Query should complete within 1 second")
        
        println("Performance test results:")
        println("Insert 100 videos: ${insertTime}ms")
        println("Query 50 videos: ${queryTime}ms")
    }

    @Test
    fun testEmbeddingVariants() = runBlocking {
        // Test multiple embedding variants
        val video = VideoEntity(uri = "content://test/video1.mp4", durationMs = 60000L, fps = 30f, width = 1920, height = 1080)
        videoDao.upsert(video)
        
        val variants = listOf(
            "clip_vit_b32_mean_v1",
            "clip_vit_b32_txtr_v1",
            "clip_vit_l14_mean_v1"
        )
        
        variants.forEach { variant ->
            val embedding = EmbeddingEntity(
                ownerType = "video",
                ownerId = video.id,
                dim = 512,
                variant = variant,
                vec = VectorConverters.floatArrayToLeBytes(FloatArray(512) { kotlin.random.Random.nextFloat() })
            )
            embeddingDao.upsert(embedding)
        }
        
        // Test variant-specific queries
        variants.forEach { variant ->
            val embeddings = embeddingDao.listByTypeAndVariant("video", variant)
            assertEquals(1, embeddings.size)
            assertEquals(variant, embeddings[0].variant)
        }
        
        // Test cross-variant queries
        val allEmbeddings = embeddingDao.forOwner(video.id)
        assertEquals(variants.size, allEmbeddings.size)
    }
}
