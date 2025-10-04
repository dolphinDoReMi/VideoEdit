package com.mira.clip

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mira.clip.clip.ClipEngines
import com.mira.clip.services.RetrievalService
import com.mira.clip.storage.FileEmbeddingStore
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@RunWith(AndroidJUnit4::class)
class VideoEmbeddingE2EInstrumentedTest {

    @Test
    fun generatesEmbeddingFromLocalVideo_andRetrievesSelf() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext

        // --- 1) Ensure real model assets are present (fail fast with a helpful message)
        try {
            // Quick load probe via a tiny text encode; forces Module.load + tokenizer load
            ClipEngines.embedText(ctx, "probe")
        } catch (t: Throwable) {
            fail("Real CLIP assets required. Provide non-placeholder assets in app/src/main/assets/: " +
                 "clip_image_encoder.ptl, clip_text_encoder.ptl, vocab.json, merges.txt, tokenizer_config.json. Cause: ${t.message}")
        }

        // --- 2) Pick a video source (sdcard preferred, else raw resource)
        val sdcardPath = "/sdcard/Movies/video_v1.mp4"
        val useSdcard = File(sdcardPath).let { it.exists() && it.length() > 0L }
        val retriever = MediaMetadataRetriever()
        if (useSdcard) {
            retriever.setDataSource(sdcardPath)
        } else {
            val resId = ctx.resources.getIdentifier("testclip", "raw", ctx.packageName)
            require(resId != 0) {
                "Missing fallback test video: place a small mp4 at app/src/androidTest/res/raw/testclip.mp4"
            }
            val uri: Uri = Uri.parse("android.resource://${ctx.packageName}/$resId")
            retriever.setDataSource(ctx, uri)
        }

        // --- 3) Uniform frame sampling (robust and minimal)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        require(durationMs > 0L) { "Video duration is 0; invalid test clip." }
        val rotation = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt() ?: 0)
        val frameCount = 16 // lighter for tests; your config uses 32

        val frames = ArrayList<Bitmap>(frameCount)
        for (i in 0 until frameCount) {
            val tMs = if (frameCount == 1) 0L else (i * (durationMs - 1) / (frameCount - 1))
            val frame = retriever.getFrameAtTime(tMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST) // timeUs
            if (frame != null) {
                frames += if (rotation != 0) rotate(frame, rotation) else frame
            }
        }
        retriever.release()
        require(frames.isNotEmpty()) { "No frames decoded from video" }

        // --- 4) Encode per-frame with CLIP, mean-pool, then L2 normalize
        val D: Int
        run {
            val first = ClipEngines.embedImage(ctx, frames[0])
            D = first.size
            require(D > 0) { "Empty embedding" }
        }

        val acc = FloatArray(D)
        for (bmp in frames) {
            val v = ClipEngines.embedImage(ctx, bmp) // returns normalized frame vec; we'll re-pool + re-L2
            for (d in 0 until D) acc[d] += v[d]
        }
        for (d in 0 until D) acc[d] /= frames.size.toFloat()
        val videoVec = ClipEngines.normalizeEmbedding(acc) // final L2 on pooled vector

        // --- 5) Persist as auditable bytes and verify sidecar
        val id = if (useSdcard) "local_video_v1" else "raw_testclip"
        val variant = "clip_vit_b32_mean_v1"
        val embeddingStore = FileEmbeddingStore()
        
        val metadata = mapOf(
            "id" to id,
            "dim" to D,
            "model" to variant,
            "frameCount" to frames.size,
            "durationMs" to durationMs,
            "timestamp" to System.currentTimeMillis(),
            "l2norm" to sqrt(videoVec.sumOf { it.toDouble() * it.toDouble() }.toFloat()),
            "sha256" to "placeholder_sha256_64_chars_long_hex_string_for_testing_purposes_only"
        )
        
        embeddingStore.storeEmbedding(variant, id, videoVec, metadata)
        
        // Verify the embedding was stored
        assertTrue("Embedding not stored", embeddingStore.hasEmbedding(variant, id))
        
        // Verify metadata
        val loadedMetadata = embeddingStore.loadMetadata(variant, id)
        assertNotNull("Failed to load metadata", loadedMetadata)
        assertEquals("Dimension mismatch", D, loadedMetadata!!["dim"])
        assertEquals("Model mismatch", variant, loadedMetadata["model"])
        assertEquals("ID mismatch", id, loadedMetadata["id"])

        // Verify JSON sidecar consistency
        val variantDir = File(File("/sdcard/MiraClip/out/embeddings"), variant)
        val jsFile = File(variantDir, "$id.json")
        assertTrue("JSON sidecar not created", jsFile.exists())
        
        val js = JSONObject(jsFile.readText())
        assertEquals(id, js.getString("id"))
        assertEquals(D, js.getInt("dim"))
        assertEquals(variant, js.getString("model"))

        // --- 6) Retrieval: self-query should be top-1 with cosine ~1.0
        val retr = RetrievalService(ctx)
        val top = retr.topK(videoVec, k = 1)
        assertTrue("No retrieval results", top.isNotEmpty())
        assertEquals("Self-match not top result", id, top[0].first)
        assertTrue("Self cosine should be close to 1; got=${top[0].second}", top[0].second > 0.99f)
    }

    // --- helpers ---
    private fun rotate(src: Bitmap, degrees: Int): Bitmap {
        val m = Matrix()
        m.postRotate(degrees.toFloat())
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }
}
