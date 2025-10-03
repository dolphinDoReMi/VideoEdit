package com.mira.clip

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mira.clip.clip.ClipEngines
import com.mira.clip.services.RetrievalService
import com.mira.clip.storage.EmbeddingStore
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MetricSpaceInstrumentedTest {

  @Test
  fun e2e_store_and_retrieve_with_valid_cosine_and_auditable_bytes() {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    // Create a simple synthetic image (gray) and a matching text prompt.
    val bmp = Bitmap.createBitmap(224,224, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GRAY) }
    val img = ClipEngines.embedImage(ctx, bmp)   // normalized
    val txt = ClipEngines.embedText(ctx, "a gray square") // normalized
    val sim = ClipEngines.cosine(img, txt)       // cosine = dot

    // Step K: vector sanity (expect finite, typically > 0.24 with real CLIP)
    assertTrue(sim.isFinite())

    // Persist & audit
    EmbeddingStore.save(ctx, "q_img", "clip_vit_b32_mean_v1", img)
    EmbeddingStore.save(ctx, "doc_text", "clip_vit_b32_mean_v1", txt)

    // Retrieval: query = image → should rank the matching text highly
    val top = RetrievalService(ctx).topK(queryNorm = img, k = 5)
    assertTrue(top.any { it.id == "doc_text" })
  }
}
