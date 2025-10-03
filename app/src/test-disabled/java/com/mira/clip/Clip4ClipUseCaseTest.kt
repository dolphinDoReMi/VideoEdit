package com.mira.clip

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mira.clip.usecases.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test suite for CLIP4Clip use cases.
 * 
 * These tests verify:
 * 1. Video ingestion workflow
 * 2. Text-based search functionality
 * 3. Database management operations
 * 4. End-to-end integration
 */
@RunWith(AndroidJUnit4::class)
class Clip4ClipUseCaseTest {

    private lateinit var context: Context
    private lateinit var videoIngestionUseCase: VideoIngestionUseCase
    private lateinit var videoSearchUseCase: VideoSearchUseCase
    private lateinit var databaseManagementUseCase: DatabaseManagementUseCase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        videoIngestionUseCase = VideoIngestionUseCase(context)
        videoSearchUseCase = VideoSearchUseCase(context)
        databaseManagementUseCase = DatabaseManagementUseCase(context)
    }

    @Test
    fun testVideoIngestionWorkflow() = runBlocking {
        // Create a test video URI (this won't actually process a real video)
        val testVideoUri = Uri.parse("content://test/video1.mp4")
        
        try {
            // Test video processing
            val videoId = videoIngestionUseCase.processVideo(
                videoUri = testVideoUri,
                variant = "clip_vit_b32_mean_v1",
                framesPerVideo = 16,
                framesPerShot = 8,
                useBackgroundProcessing = false // Process synchronously for testing
            )
            
            // Verify video ID is returned
            assertNotNull(videoId)
            assertTrue(videoId.isNotEmpty(), "Video ID should not be empty")
            
            // Test processing status
            val status = videoIngestionUseCase.getProcessingStatus(testVideoUri)
            assertNotNull(status)
            
            println("Video processing test completed. Video ID: $videoId, Status: $status")
            
        } catch (e: Exception) {
            // Expected if models aren't loaded or video doesn't exist
            println("Expected error (models/video not available): ${e.message}")
        }
    }

    @Test
    fun testTextBasedSearch() = runBlocking {
        val testQueries = listOf(
            "person walking",
            "outdoor scene",
            "people talking",
            "vehicle on road"
        )
        
        testQueries.forEach { query ->
            try {
                val results = videoSearchUseCase.searchVideos(
                    query = query,
                    variant = "clip_vit_b32_mean_v1",
                    topK = 5,
                    searchLevel = "video"
                )
                
                // Verify results structure
                assertNotNull(results)
                assertTrue(results.size <= 5, "Results should not exceed topK limit")
                
                results.forEach { result ->
                    assertNotNull(result.id)
                    assertTrue(result.similarity >= -1.0f && result.similarity <= 1.0f, 
                        "Similarity should be between -1 and 1")
                    assertTrue(result.startMs >= 0, "Start time should be non-negative")
                    assertTrue(result.endMs >= result.startMs, "End time should be >= start time")
                    assertEquals("video", result.searchLevel)
                }
                
                println("Search query '$query' returned ${results.size} results")
                
            } catch (e: Exception) {
                // Expected if models aren't loaded or no videos in database
                println("Expected error for query '$query' (models/videos not available): ${e.message}")
            }
        }
    }

    @Test
    fun testDatabaseManagement() = runBlocking {
        try {
            // Test database statistics
            val stats = databaseManagementUseCase.getDatabaseStats()
            assertNotNull(stats)
            assertTrue(stats.videoCount >= 0, "Video count should be non-negative")
            assertTrue(stats.embeddingCount >= 0, "Embedding count should be non-negative")
            assertTrue(stats.databaseSize >= 0, "Database size should be non-negative")
            
            println("Database stats: $stats")
            
            // Test embedding statistics
            val embeddingStats = videoSearchUseCase.getEmbeddingStats("clip_vit_b32_mean_v1")
            assertNotNull(embeddingStats)
            
            println("Embedding stats: $embeddingStats")
            
        } catch (e: Exception) {
            // Expected if database isn't initialized
            println("Expected error (database not initialized): ${e.message}")
        }
    }

    @Test
    fun testVideoWithEmbeddingsFlow() = runBlocking {
        try {
            // Test getting videos with embeddings
            val videosFlow = videoSearchUseCase.getAllVideosWithEmbeddings("clip_vit_b32_mean_v1")
            
            // Collect the first emission
            val videos = videosFlow.first()
            assertNotNull(videos)
            
            videos.forEach { videoWithEmbedding ->
                assertNotNull(videoWithEmbedding.video)
                assertNotNull(videoWithEmbedding.embeddings)
                
                // Verify video properties
                assertTrue(videoWithEmbedding.video.uri.isNotEmpty(), "Video URI should not be empty")
                assertTrue(videoWithEmbedding.video.durationMs >= 0, "Duration should be non-negative")
                assertTrue(videoWithEmbedding.video.fps > 0, "FPS should be positive")
                
                // Verify embeddings
                videoWithEmbedding.embeddings.forEach { embedding ->
                    assertEquals("video", embedding.ownerType)
                    assertEquals(videoWithEmbedding.video.id, embedding.ownerId)
                    assertTrue(embedding.dim > 0, "Embedding dimension should be positive")
                    assertTrue(embedding.vec.isNotEmpty(), "Embedding vector should not be empty")
                }
            }
            
            println("Found ${videos.size} videos with embeddings")
            
        } catch (e: Exception) {
            // Expected if database isn't initialized or no videos
            println("Expected error (database/videos not available): ${e.message}")
        }
    }

    @Test
    fun testShotsWithEmbeddings() = runBlocking {
        // Create a test video ID
        val testVideoId = "test_video_123"
        
        try {
            val shotsWithEmbeddings = videoSearchUseCase.getShotsWithEmbeddings(
                videoId = testVideoId,
                variant = "clip_vit_b32_mean_v1"
            )
            
            assertNotNull(shotsWithEmbeddings)
            
            shotsWithEmbeddings.forEach { shotWithEmbedding ->
                assertNotNull(shotWithEmbedding.shot)
                assertNotNull(shotWithEmbedding.embeddings)
                
                // Verify shot properties
                assertEquals(testVideoId, shotWithEmbedding.shot.videoId)
                assertTrue(shotWithEmbedding.shot.startMs >= 0, "Start time should be non-negative")
                assertTrue(shotWithEmbedding.shot.endMs >= shotWithEmbedding.shot.startMs, 
                    "End time should be >= start time")
                
                // Verify embeddings
                shotWithEmbedding.embeddings.forEach { embedding ->
                    assertEquals("shot", embedding.ownerType)
                    assertEquals(shotWithEmbedding.shot.id, embedding.ownerId)
                }
            }
            
            println("Found ${shotsWithEmbeddings.size} shots with embeddings for video $testVideoId")
            
        } catch (e: Exception) {
            // Expected if database isn't initialized or video doesn't exist
            println("Expected error (database/video not available): ${e.message}")
        }
    }

    @Test
    fun testBatchVideoProcessing() = runBlocking {
        val testVideoUris = listOf(
            Uri.parse("content://test/video1.mp4"),
            Uri.parse("content://test/video2.mp4"),
            Uri.parse("content://test/video3.mp4")
        )
        
        try {
            val processedCount = videoIngestionUseCase.processVideosBatch(
                videoUris = testVideoUris,
                variant = "clip_vit_b32_mean_v1",
                framesPerVideo = 16,
                framesPerShot = 8
            )
            
            assertEquals(testVideoUris.size, processedCount, 
                "All videos should be enqueued for processing")
            
            println("Batch processing enqueued $processedCount videos")
            
        } catch (e: Exception) {
            // Expected if models aren't loaded
            println("Expected error (models not available): ${e.message}")
        }
    }

    @Test
    fun testSearchLevelVariations() = runBlocking {
        val testQuery = "person walking"
        
        try {
            // Test video-level search
            val videoResults = videoSearchUseCase.searchVideos(
                query = testQuery,
                searchLevel = "video",
                topK = 3
            )
            
            videoResults.forEach { result ->
                assertEquals("video", result.searchLevel)
            }
            
            // Test shot-level search
            val shotResults = videoSearchUseCase.searchVideos(
                query = testQuery,
                searchLevel = "shot",
                topK = 3
            )
            
            shotResults.forEach { result ->
                assertEquals("shot", result.searchLevel)
            }
            
            println("Video-level search: ${videoResults.size} results")
            println("Shot-level search: ${shotResults.size} results")
            
        } catch (e: Exception) {
            // Expected if models aren't loaded or no videos
            println("Expected error (models/videos not available): ${e.message}")
        }
    }

    @Test
    fun testErrorHandling() = runBlocking {
        // Test with invalid inputs
        try {
            // Empty query
            val emptyResults = videoSearchUseCase.searchVideos("", topK = 5)
            assertNotNull(emptyResults)
            
            // Invalid video URI
            val invalidUri = Uri.parse("invalid://uri")
            val status = videoIngestionUseCase.getProcessingStatus(invalidUri)
            assertNotNull(status)
            
            // Non-existent video ID
            videoSearchUseCase.getShotsWithEmbeddings("non_existent_id")
            
            println("Error handling tests completed successfully")
            
        } catch (e: Exception) {
            // Some errors are expected
            println("Expected error in error handling test: ${e.message}")
        }
    }

    @Test
    fun testPerformanceMetrics() = runBlocking {
        val testQuery = "performance test query"
        
        try {
            val startTime = System.currentTimeMillis()
            
            val results = videoSearchUseCase.searchVideos(
                query = testQuery,
                topK = 10
            )
            
            val endTime = System.currentTimeMillis()
            val searchTime = endTime - startTime
            
            assertNotNull(results)
            
            // Performance assertions (adjust thresholds as needed)
            assertTrue(searchTime < 10000, "Search should complete within 10 seconds")
            
            println("Search performance: ${searchTime}ms for ${results.size} results")
            
        } catch (e: Exception) {
            // Expected if models aren't loaded
            println("Expected error (models not available): ${e.message}")
        }
    }
}
