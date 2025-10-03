package com.mira.com

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ClipE2E {
  @Test
  fun clipBroadcastProducesArtifacts() {
    val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    val ins = InstrumentationRegistry.getInstrumentation().context.assets.open("tiny.mp4")
    val input = File(ctx.filesDir, "tiny.mp4").also { it.outputStream().use(ins::copyTo) }
    val out = File(ctx.filesDir, "clip_out").apply { mkdirs() }

    val intent = Intent("${ctx.packageName}.CLIP.RUN").apply {
      setPackage(ctx.packageName)
      putExtra("input", Uri.fromFile(input).toString())
      putExtra("outdir", Uri.fromFile(out).toString())
      putExtra("variant", "ViT-B_32")
      putExtra("frame_count", 8)
    }
    ctx.sendBroadcast(intent)
    Thread.sleep(1200)

    val vec = File(out, "embeddings/ViT-B_32/tiny.f32")
    val meta = File(out, "embeddings/ViT-B_32/tiny.json")
    assertTrue(vec.exists() && vec.length() > 0)
    assertTrue(meta.exists() && meta.readText().contains("\"variant\""))
  }
}
