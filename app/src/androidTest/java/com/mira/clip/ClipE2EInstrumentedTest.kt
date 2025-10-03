package com.mira.clip

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mira.clip.usecases.ComputeClipSimilarityUseCase
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClipE2EInstrumentedTest {

  @Test
  fun image_text_similarity_reasonable() {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    val uc = ComputeClipSimilarityUseCase(ctx)
    val ins = ctx.resources.openRawResource(
      ctx.resources.getIdentifier("dog", "drawable", ctx.packageName)
    )
    val bmp = BitmapFactory.decodeStream(ins)
    val (iv, tv) = uc.run(bmp, "a photo of a dog")
    val sim = uc.cosine(iv, tv)
    // With real model & tokenizer, this should be comfortably > 0.24
    assertTrue("cosine sim too low: $sim", sim > 0.24f)
  }
}
