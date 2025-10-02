package com.mira.clip

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.mira.clip.db.AppDatabase
import com.mira.clip.workers.VideoIngestWorker
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class IngestWorkerInstrumentedTest {
    private lateinit var ctx: Context

    @Before
    fun setup() {
        ctx = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder().build()
        WorkManagerTestInitHelper.initializeTestWorkManager(ctx, config)
    }

    @Test
    fun video_is_ingested_and_embedding_written() = runBlocking {
        val uri = copyRawToFileUri(ctx, "sample.mp4")
        val req = OneTimeWorkRequestBuilder<VideoIngestWorker>()
            .addTag("VideoIngest")
            .setInputData(androidx.work.workDataOf("video_uri" to uri.toString()))
            .build()
        
        val wm = WorkManager.getInstance(ctx)
        wm.enqueue(req).result.get()

        // Block until finished (test env)
        WorkManagerTestInitHelper.getTestDriver(ctx)!!.setAllConstraintsMet(req.id)
        
        // Wait for completion
        var tries = 0
        var workInfo = wm.getWorkInfoById(req.id).get()
        while (workInfo.state !in setOf(WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED) && tries++ < 50) {
            TimeUnit.MILLISECONDS.sleep(100)
            workInfo = wm.getWorkInfoById(req.id).get()
        }
        
        assertEquals("Work should succeed", WorkInfo.State.SUCCEEDED, workInfo.state)

        // Verify DB side-effect
        val db = AppDatabase.get(ctx)
        val count = db.embeddingDao().listByTypeAndVariant("video", "clip_vit_b32_mean_v1").size
        assertTrue("At least 1 embedding should be written", count >= 1)
    }

    @Test
    fun worker_handles_invalid_uri_gracefully() = runBlocking {
        val req = OneTimeWorkRequestBuilder<VideoIngestWorker>()
            .addTag("VideoIngest")
            .setInputData(androidx.work.workDataOf("video_uri" to "invalid_uri"))
            .build()
        
        val wm = WorkManager.getInstance(ctx)
        wm.enqueue(req).result.get()

        // Block until finished
        WorkManagerTestInitHelper.getTestDriver(ctx)!!.setAllConstraintsMet(req.id)
        
        var tries = 0
        var workInfo = wm.getWorkInfoById(req.id).get()
        while (workInfo.state !in setOf(WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED) && tries++ < 50) {
            TimeUnit.MILLISECONDS.sleep(100)
            workInfo = wm.getWorkInfoById(req.id).get()
        }
        
        // Should fail gracefully
        assertEquals("Work should fail with invalid URI", WorkInfo.State.FAILED, workInfo.state)
    }

    @Test
    fun worker_with_custom_parameters() = runBlocking {
        val uri = copyRawToFileUri(ctx, "sample.mp4")
        val req = OneTimeWorkRequestBuilder<VideoIngestWorker>()
            .addTag("VideoIngest")
            .setInputData(androidx.work.workDataOf(
                "video_uri" to uri.toString(),
                "variant" to "clip_vit_b32_mean_v1",
                "frames_per_video" to 16,
                "frames_per_shot" to 8
            ))
            .build()
        
        val wm = WorkManager.getInstance(ctx)
        wm.enqueue(req).result.get()

        // Block until finished
        WorkManagerTestInitHelper.getTestDriver(ctx)!!.setAllConstraintsMet(req.id)
        
        var tries = 0
        var workInfo = wm.getWorkInfoById(req.id).get()
        while (workInfo.state !in setOf(WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED) && tries++ < 50) {
            TimeUnit.MILLISECONDS.sleep(100)
            workInfo = wm.getWorkInfoById(req.id).get()
        }
        
        assertEquals("Work should succeed with custom parameters", WorkInfo.State.SUCCEEDED, workInfo.state)

        // Verify output data
        val outputData = workInfo.outputData
        assertNotNull("Output data should not be null", outputData)
        assertTrue("Should contain video_id", outputData.keyValueMap.containsKey("video_id"))
        assertTrue("Should contain variant", outputData.keyValueMap.containsKey("variant"))
        assertEquals("Should contain correct variant", "clip_vit_b32_mean_v1", outputData.getString("variant"))
    }

    @Test
    fun worker_progress_updates() = runBlocking {
        val uri = copyRawToFileUri(ctx, "sample.mp4")
        val req = OneTimeWorkRequestBuilder<VideoIngestWorker>()
            .addTag("VideoIngest")
            .setInputData(androidx.work.workDataOf("video_uri" to uri.toString()))
            .build()
        
        val wm = WorkManager.getInstance(ctx)
        wm.enqueue(req).result.get()

        // Block until finished
        WorkManagerTestInitHelper.getTestDriver(ctx)!!.setAllConstraintsMet(req.id)
        
        var tries = 0
        var workInfo = wm.getWorkInfoById(req.id).get()
        while (workInfo.state !in setOf(WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED) && tries++ < 50) {
            TimeUnit.MILLISECONDS.sleep(100)
            workInfo = wm.getWorkInfoById(req.id).get()
        }
        
        assertEquals("Work should succeed", WorkInfo.State.SUCCEEDED, workInfo.state)

        // Check that progress was reported
        val progressData = workInfo.progress
        assertNotNull("Progress data should not be null", progressData)
    }

    @Test
    fun worker_database_integration() = runBlocking {
        val uri = copyRawToFileUri(ctx, "sample.mp4")
        val req = OneTimeWorkRequestBuilder<VideoIngestWorker>()
            .addTag("VideoIngest")
            .setInputData(androidx.work.workDataOf("video_uri" to uri.toString()))
            .build()
        
        val wm = WorkManager.getInstance(ctx)
        wm.enqueue(req).result.get()

        // Block until finished
        WorkManagerTestInitHelper.getTestDriver(ctx)!!.setAllConstraintsMet(req.id)
        
        var tries = 0
        var workInfo = wm.getWorkInfoById(req.id).get()
        while (workInfo.state !in setOf(WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED) && tries++ < 50) {
            TimeUnit.MILLISECONDS.sleep(100)
            workInfo = wm.getWorkInfoById(req.id).get()
        }
        
        assertEquals("Work should succeed", WorkInfo.State.SUCCEEDED, workInfo.state)

        // Verify database integration
        val db = AppDatabase.get(ctx)
        
        // Check that video entity was created
        val videos = db.videoDao().getLatest(10)
        assertTrue("Should have at least one video", videos.isNotEmpty())
        
        // Check that embedding was created
        val embeddings = db.embeddingDao().listByTypeAndVariant("video", "clip_vit_b32_mean_v1")
        assertTrue("Should have at least one embedding", embeddings.isNotEmpty())
        
        // Verify embedding properties
        val embedding = embeddings.first()
        assertEquals("Owner type should be video", "video", embedding.ownerType)
        assertTrue("Embedding dimension should be > 0", embedding.dim > 0)
        assertTrue("Embedding vector should not be empty", embedding.vec.isNotEmpty())
    }

    @Test
    fun worker_concurrent_execution() = runBlocking {
        val uri1 = copyRawToFileUri(ctx, "sample.mp4")
        val uri2 = copyRawToFileUri(ctx, "sample.mp4")
        
        val req1 = OneTimeWorkRequestBuilder<VideoIngestWorker>()
            .addTag("VideoIngest")
            .setInputData(androidx.work.workDataOf("video_uri" to uri1.toString()))
            .build()
        
        val req2 = OneTimeWorkRequestBuilder<VideoIngestWorker>()
            .addTag("VideoIngest")
            .setInputData(androidx.work.workDataOf("video_uri" to uri2.toString()))
            .build()
        
        val wm = WorkManager.getInstance(ctx)
        wm.enqueue(req1).result.get()
        wm.enqueue(req2).result.get()

        // Block until both finished
        WorkManagerTestInitHelper.getTestDriver(ctx)!!.setAllConstraintsMet(req1.id)
        WorkManagerTestInitHelper.getTestDriver(ctx)!!.setAllConstraintsMet(req2.id)
        
        var tries = 0
        var workInfo1 = wm.getWorkInfoById(req1.id).get()
        var workInfo2 = wm.getWorkInfoById(req2.id).get()
        
        while ((workInfo1.state !in setOf(WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED) ||
                workInfo2.state !in setOf(WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED)) && tries++ < 100) {
            TimeUnit.MILLISECONDS.sleep(100)
            workInfo1 = wm.getWorkInfoById(req1.id).get()
            workInfo2 = wm.getWorkInfoById(req2.id).get()
        }
        
        assertEquals("First work should succeed", WorkInfo.State.SUCCEEDED, workInfo1.state)
        assertEquals("Second work should succeed", WorkInfo.State.SUCCEEDED, workInfo2.state)

        // Verify both embeddings were created
        val db = AppDatabase.get(ctx)
        val embeddings = db.embeddingDao().listByTypeAndVariant("video", "clip_vit_b32_mean_v1")
        assertTrue("Should have at least 2 embeddings", embeddings.size >= 2)
    }
}

// Utility function to copy raw resource to file and return Uri
fun copyRawToFileUri(ctx: Context, name: String): Uri {
    val id = ctx.resources.getIdentifier(name.substringBefore("."), "raw", ctx.packageName)
    require(id != 0) { "raw/$name not found" }
    val out = java.io.File(ctx.filesDir, name)
    ctx.resources.openRawResource(id).use { input ->
        out.outputStream().use { input.copyTo(it) }
    }
    return Uri.fromFile(out)
}
