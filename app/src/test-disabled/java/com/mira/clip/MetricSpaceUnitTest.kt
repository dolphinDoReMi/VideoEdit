package com.mira.clip

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mira.clip.clip.ClipEngines
import com.mira.clip.storage.EmbeddingStore
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MetricSpaceUnitTest {

  @Test
  fun normalization_is_unit_length_and_idempotent() {
    val v = floatArrayOf(3f, 4f, 0f)
    val n1 = ClipEngines.normalizeEmbedding(v)
    val n2 = ClipEngines.normalizeEmbedding(n1)
    fun l2(x: FloatArray): Float = kotlin.math.sqrt(x.sumOf { (it*it).toDouble() }.toFloat())
    assertEquals(1f, l2(n1), 1e-4f)
    // idempotent
    assertArrayEquals(n1, n2, 1e-6f)
  }

  @Test
  fun f32_le_binary_roundtrip_and_endianness() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val dir = File(ctx.filesDir, "embeddings_test").apply { mkdirs() }
    val id = "le_probe"
    val path = File(dir, "$id.f32")

    // bytes for [1.0f, 2.0f] in LE: 00 00 80 3F, 00 00 00 40
    val bb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
    bb.putFloat(1.0f); bb.putFloat(2.0f)
    path.writeBytes(bb.array())

    val got = ByteBuffer.wrap(path.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
    val a = got.float; val b = got.float
    assertEquals(1.0f, a, 0f); assertEquals(2.0f, b, 0f)
  }

  @Test
  fun save_and_load_preserves_bytes_and_meta() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val v = floatArrayOf(0.6f, 0.8f) // already unit
    val meta = EmbeddingStore.save(ctx, "unit_vec", "clip_vit_b32_mean_v1", v)
    assertEquals(2, meta.dim)
    assertEquals("f32", meta.dtype)
    assertEquals("LE", meta.endian)
    assertEquals(1f, meta.l2norm, 1e-4f)

    val loaded = EmbeddingStore.load(ctx, "unit_vec", expectedDim = 2)
    assertArrayEquals(v, loaded, 1e-6f)
  }
}
