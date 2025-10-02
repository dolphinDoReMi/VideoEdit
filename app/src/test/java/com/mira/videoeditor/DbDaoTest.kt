package com.mira.videoeditor

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mira.videoeditor.db.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DbDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `videos-embeddings roundtrip & constraints`() = runBlocking {
        val v = VideoEntity(
            id = "v1", 
            uri = "content://v1", 
            durationMs = 1000, 
            fps = 30f, 
            width = 1920, 
            height = 1080
        )
        db.videoDao().upsert(v)
        
        val e = EmbeddingEntity(
            ownerType = "video", 
            ownerId = "v1", 
            dim = 512, 
            variant = "clip_vit_b32_mean_v1",
            vec = ByteArray(512 * 4) { 0 } // zero ok for schema check
        )
        db.embeddingDao().upsert(e)
        
        val rows = db.readModelsDao().videosWithEmbeddings("clip_vit_b32_mean_v1")
        assertEquals(1, rows.size)
        assertEquals("v1", rows.first().video.id)
    }

    @Test
    fun `video dao operations`() = runBlocking {
        val video = VideoEntity(
            id = "test_video",
            uri = "content://test",
            durationMs = 5000,
            fps = 30f,
            width = 1920,
            height = 1080
        )

        // Insert
        db.videoDao().upsert(video)
        
        // Retrieve by ID
        val retrieved = db.videoDao().getById("test_video")
        assertNotNull(retrieved)
        assertEquals("test_video", retrieved?.id)
        assertEquals("content://test", retrieved?.uri)
        
        // Retrieve by URI
        val byUri = db.videoDao().getByUri("content://test")
        assertNotNull(byUri)
        assertEquals("test_video", byUri?.id)
        
        // Delete
        db.videoDao().deleteById("test_video")
        val deleted = db.videoDao().getById("test_video")
        assertNull(deleted)
    }

    @Test
    fun `embedding dao operations`() = runBlocking {
        val embedding = EmbeddingEntity(
            ownerType = "video",
            ownerId = "test_video",
            dim = 512,
            variant = "clip_vit_b32_mean_v1",
            vec = ByteArray(512 * 4) { (it % 256).toByte() }
        )

        // Insert
        db.embeddingDao().upsert(embedding)
        
        // Retrieve by owner
        val byOwner = db.embeddingDao().forOwner("test_video")
        assertEquals(1, byOwner.size)
        assertEquals("test_video", byOwner.first().ownerId)
        
        // Retrieve by owner and variant
        val byOwnerAndVariant = db.embeddingDao().forOwnerAndVariant("test_video", "clip_vit_b32_mean_v1")
        assertNotNull(byOwnerAndVariant)
        assertEquals(512, byOwnerAndVariant?.dim)
        
        // List by type and variant
        val byTypeAndVariant = db.embeddingDao().listByTypeAndVariant("video", "clip_vit_b32_mean_v1")
        assertEquals(1, byTypeAndVariant.size)
        
        // Count by variant
        val count = db.embeddingDao().countByVariant("clip_vit_b32_mean_v1")
        assertEquals(1, count)
    }

    @Test
    fun `shot dao operations`() = runBlocking {
        val video = VideoEntity(
            id = "test_video",
            uri = "content://test",
            durationMs = 10000,
            fps = 30f,
            width = 1920,
            height = 1080
        )
        db.videoDao().upsert(video)

        val shot = ShotEntity(
            id = "shot1",
            videoId = "test_video",
            startMs = 0,
            endMs = 2000
        )

        // Insert
        db.shotDao().upsert(shot)
        
        // Retrieve by video
        val shots = db.shotDao().forVideo("test_video")
        assertEquals(1, shots.size)
        assertEquals("shot1", shots.first().id)
        
        // Retrieve by range
        val shotsInRange = db.shotDao().forVideoInRange("test_video", 0, 3000)
        assertEquals(1, shotsInRange.size)
        
        // Delete by video
        db.shotDao().deleteByVideoId("test_video")
        val shotsAfterDelete = db.shotDao().forVideo("test_video")
        assertEquals(0, shotsAfterDelete.size)
    }

    @Test
    fun `text dao operations`() = runBlocking {
        val text = TextEntity(
            id = "text1",
            text = "a photo of a cat",
            lang = "en"
        )

        // Insert
        db.textDao().upsert(text)
        
        // Retrieve by ID
        val retrieved = db.textDao().getById("text1")
        assertNotNull(retrieved)
        assertEquals("a photo of a cat", retrieved?.text)
        
        // Search by text
        val searchResults = db.textDao().searchByText("cat", 10)
        assertEquals(1, searchResults.size)
        assertEquals("text1", searchResults.first().id)
        
        // Delete
        db.textDao().delete(text)
        val deleted = db.textDao().getById("text1")
        assertNull(deleted)
    }

    @Test
    fun `unique constraint enforcement`() = runBlocking {
        val embedding1 = EmbeddingEntity(
            ownerType = "video",
            ownerId = "test_video",
            dim = 512,
            variant = "clip_vit_b32_mean_v1",
            vec = ByteArray(512 * 4) { 1 }
        )

        val embedding2 = EmbeddingEntity(
            ownerType = "video",
            ownerId = "test_video",
            dim = 512,
            variant = "clip_vit_b32_mean_v1",
            vec = ByteArray(512 * 4) { 2 }
        )

        // Insert first embedding
        db.embeddingDao().upsert(embedding1)
        
        // Insert second embedding with same ownerType, ownerId, variant
        // This should replace the first one due to unique constraint
        db.embeddingDao().upsert(embedding2)
        
        // Should only have one embedding
        val embeddings = db.embeddingDao().forOwnerAndVariant("test_video", "clip_vit_b32_mean_v1")
        assertNotNull(embeddings)
        assertEquals(2, embeddings?.vec?.get(0)) // Should be the second embedding's data
    }
}
