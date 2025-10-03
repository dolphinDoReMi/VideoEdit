package com.mira.clip

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.mira.clip.clip.ClipEngines
import com.mira.clip.storage.FileEmbeddingStore
import com.mira.clip.video.FrameSampler
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.sqrt

@RunWith(AndroidJUnit4::class)
@LargeTest
class E2EVideoEmbeddingTest {

    @get:Rule
    val storagePerms: GrantPermissionRule = if (Build.VERSION.SDK_INT >= 33) {
        GrantPermissionRule.grant(
            Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        GrantPermissionRule.grant(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    private val TAG = "E2EVideoEmbeddingTest"

    private val EXPECTED_DIM = 512
    private val DEFAULT_MODEL_ID = "clip_vit_b32_mean_v1"
    private val DEFAULT_FRAME_COUNT = 8  // Reduced for faster testing
    private val DEFAULT_VIDEO_PATH = "/sdcard/Movies/video_v1.mp4"

    @Test
    fun generateEmbedding_fromLocalVideo_andAudit() {
        val instr = InstrumentationRegistry.getInstrumentation()
        val ctx = instr.targetContext

        val argVideoPath = DEFAULT_VIDEO_PATH
        val modelId = DEFAULT_MODEL_ID
        val frameCount = DEFAULT_FRAME_COUNT

        // Try to use the sample video from resources first
        val videoFile = try {
            val sampleVideoUri = copyRawToFileUri(ctx, "sample.mp4")
            File(sampleVideoUri.path ?: "")
        } catch (e: Exception) {
            Log.w(TAG, "Could not use sample video, trying other sources: ${e.message}")
            // Resolve an actual readable file path. If the direct path doesn't exist, try MediaStore.
            resolveVideoFile(ctx, argVideoPath)
                ?: resolveFromMediaStore(ctx, "video_v1.mp4")  // by name
                ?: resolveAnyVideoFromMediaStore(ctx)          // fallback: first decent video
                ?: error("No accessible video found. Tried path=$argVideoPath and MediaStore.")
        }

        Log.i(TAG, "Using video file: ${videoFile.absolutePath} (size=${videoFile.length()} bytes)")
        assertTrue("Test video not readable: ${videoFile.absolutePath}", videoFile.canRead())

        // 1) Sample frames
        val frames: List<Bitmap> = FrameSampler.sampleUniform(ctx, videoFile.absolutePath, frameCount)
        assertTrue("No frames sampled", frames.isNotEmpty())
        assertEquals("Unexpected frame count", frameCount, frames.size)

        // 2) Initialize CLIP engines and encode video (CLIP per-frame → mean-pool → L2)
        val clipEngines = ClipEngines(ctx)
        clipEngines.initialize()
        val vec: FloatArray = clipEngines.encodeFrames(frames)
        assertEquals("Unexpected dim", EXPECTED_DIM, vec.size)

        // 3) Sanity checks on vector
        assertFalse("Vector contains NaN", vec.any { it.isNaN() })
        assertFalse("Vector contains Inf", vec.any { it == Float.POSITIVE_INFINITY || it == Float.NEGATIVE_INFINITY })
        val norm = l2(vec)
        assertTrue("Vector not L2-normalized: norm=$norm", abs(norm - 1f) < 1e-2)
        val maxAbs = vec.maxOf { kotlin.math.abs(it) }
        assertTrue("Vector is near-zero (maxAbs=$maxAbs)", maxAbs > 1e-3f)

        // 4) Persist & audit using FileEmbeddingStore
        val id = "test_video_${System.currentTimeMillis()}"
        val embeddingStore = FileEmbeddingStore()
        val metadata = mapOf(
            "frameCount" to frameCount,
            "videoPath" to videoFile.absolutePath,
            "timestamp" to System.currentTimeMillis(),
            "dimension" to EXPECTED_DIM,
            "model" to modelId
        )
        
        embeddingStore.storeEmbedding(modelId, id, vec, metadata)
        assertTrue("Embedding not stored", embeddingStore.hasEmbedding(modelId, id))

        // 5) Reload exact bytes and compare
        val loaded = embeddingStore.loadEmbedding(modelId, id)
        assertNotNull("Failed to load embedding", loaded)
        assertEquals("Reloaded dim mismatch", EXPECTED_DIM, loaded!!.size)
        for (i in vec.indices) {
            assertTrue("Mismatch at index $i", abs(vec[i] - loaded[i]) < 1e-6f)
        }

        // 6) Verify metadata
        val loadedMetadata = embeddingStore.loadMetadata(modelId, id)
        assertNotNull("Failed to load metadata", loadedMetadata)
        assertEquals("Frame count mismatch", frameCount, loadedMetadata!!["frameCount"])
        assertEquals("Dimension mismatch", EXPECTED_DIM, loadedMetadata["dimension"])

        // 7) Log a tiny digest for debugging
        Log.i(TAG, "OK: dim=$EXPECTED_DIM, norm=$norm, maxAbs=$maxAbs, id=$id, model=$modelId")
    }

    // ---------- helpers ----------

    private fun l2(v: FloatArray): Float {
        var s = 0f
        for (x in v) s += x * x
        return sqrt(s)
    }

    private fun copyRawToFileUri(ctx: Context, name: String): Uri {
        val cache = File(ctx.cacheDir, "e2e_videos")
        if (!cache.exists()) cache.mkdirs()
        val out = File(cache, name)
        
        ctx.resources.openRawResource(
            ctx.resources.getIdentifier(name.substringBefore("."), "raw", ctx.packageName)
        ).use { input ->
            out.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        return Uri.fromFile(out)
    }

    private fun resolveVideoFile(ctx: Context, path: String): File? {
        val f = File(path)
        return if (f.exists() && f.canRead()) f else null
    }

    private fun resolveFromMediaStore(ctx: Context, displayName: String): File? {
        val cr = ctx.contentResolver
        val uri = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE
        )

        cr.query(uri, projection, "${MediaStore.Video.Media.DISPLAY_NAME}=?",
            arrayOf(displayName), "${MediaStore.Video.Media.DATE_ADDED} DESC").use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(uri, id)
                return cacheUriToTempFile(ctx, contentUri, displayName)
            }
        }
        return null
    }

    private fun resolveAnyVideoFromMediaStore(ctx: Context): File? {
        val cr = ctx.contentResolver
        val baseUri = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE
        )

        // Prefer a file that is at least ~1MB
        cr.query(baseUri, projection, "${MediaStore.Video.Media.SIZE}>=?",
            arrayOf((1 * 1024 * 1024).toString()), "${MediaStore.Video.Media.DATE_ADDED} DESC").use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
                val contentUri = ContentUris.withAppendedId(baseUri, id)
                return cacheUriToTempFile(ctx, contentUri, name ?: "media_store_video.mp4")
            }
        }
        return null
    }

    private fun cacheUriToTempFile(ctx: Context, uri: Uri, nameHint: String): File? {
        val cache = File(ctx.cacheDir, "e2e_videos")
        if (!cache.exists()) cache.mkdirs()
        val out = File(cache, nameHint.ifBlank { "video.mp4" })

        return runCatching {
            ctx.contentResolver.openFileDescriptor(uri, "r")!!.use { pfd ->
                FileInputStream(pfd.fileDescriptor).use { ins ->
                    FileOutputStream(out).use { outs ->
                        ins.copyTo(outs)
                    }
                }
            }
            out
        }.onFailure { e ->
            Log.e(TAG, "Failed to cache URI $uri: ${e.message}", e)
        }.getOrNull()
    }
}
