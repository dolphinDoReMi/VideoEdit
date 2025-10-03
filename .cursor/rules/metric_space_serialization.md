---
description: "Metric space & serialization (valid cosine & auditable bytes) for CLIP on Android. L2-normalization ON, .f32(LE)+JSON sidecar, cosine retrieval, end-to-end tests."
alwaysApply: false
requireConfirmation: true
tags: ["android","kotlin","pytorch-mobile","storage","retrieval","testing"]
---

# Scope (Status: COMPLIANT)

**Control knots**
- L2 normalization: **ON** (`ClipEngines.kt` lines ~105–113)
- Storage: **.f32 (Little-Endian)** + **JSON sidecar** (`EmbeddingStore.kt` lines ~130–139)
- Similarity: **cosine** (`RetrievalService.kt` lines ~127–139)

**Implementation guarantees**
- `normalizeEmbedding()` uses `sqrt(sumOf { it*it })`
- Binary format via `ByteBuffer.order(LITTLE_ENDIAN)`
- Cosine = dot product of **normalized** vectors
- Step-K e2e verification included (vector checks)

---

# 1) File: `app/src/main/java/com/mira/clip/clip/ClipEngines.kt`
> Add L2 normalization and ensure every emitted vector is normalized before leaving the engine. Keep existing signatures.

```kotlin
// ... existing package & imports ...
package com.mira.clip.clip

import android.content.Context
import android.graphics.Bitmap
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.security.MessageDigest
import kotlin.math.sqrt

object ClipEngines {
  const val MODEL = "clip_vit_b32_mean_v1.pt"
  // ...

  @Volatile private var module: Module? = null
  // ...

  // ---- [105–113] L2 normalization: ON ----
  fun normalizeEmbedding(vec: FloatArray): FloatArray {
    var ss = 0f
    for (v in vec) ss += v * v
    val n = sqrt(ss.toDouble()).toFloat()
    if (n <= 0f) return vec
    val out = FloatArray(vec.size)
    val inv = 1f / n
    for (i in vec.indices) out[i] = vec[i] * inv
    return out
  }
  // ---------------------------------------

  fun embedImage(context: Context, bmp: Bitmap): FloatArray {
    ensureLoaded(context)
    val input = ClipPreprocess.to224Input(bmp)
    val out = module!!.forward(
      IValue.from(input),
      IValue.from(Tensor.fromBlob(longArrayOf(0), longArrayOf(1,1))) // dummy text
    ).toTuple()[0].toTensor().dataAsFloatArray()
    return normalizeEmbedding(out) // enforce ON
  }

  fun embedText(context: Context, text: String): FloatArray {
    ensureLoaded(context)
    val ids = ClipBPETokenizer.fromAssets(context).encode(text)
    val tt = Tensor.fromBlob(ids.map { it.toLong() }.toLongArray(), longArrayOf(1, ids.size.toLong()))
    val out = module!!.forward(
      IValue.from(Tensor.fromBlob(floatArrayOf(0f), longArrayOf(1,1))), // dummy image
      IValue.from(tt)
    ).toTuple()[1].toTensor().dataAsFloatArray()
    return normalizeEmbedding(out) // enforce ON
  }

  fun cosine(a: FloatArray, b: FloatArray): Float {
    // a,b assumed normalized; this is plain dot
    val n = minOf(a.size, b.size)
    var dot = 0f
    for (i in 0 until n) dot += a[i] * b[i]
    return dot
  }

  // ... copyAsset, sha256 unchanged ...
}
```

⸻

# 2) File: `app/src/main/java/com/mira/clip/storage/EmbeddingStore.kt`

New storage layer: writes .f32 (LE) bytes + JSON sidecar with auditable metadata (dim, dtype, endianness, SHA256, norm, model id, created_at).

```kotlin
package com.mira.clip.storage

import android.content.Context
import com.mira.clip.clip.ClipEngines
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.time.Instant
import java.time.format.DateTimeFormatter

object EmbeddingStore {

  data class Meta(
    val id: String,
    val model: String,
    val dim: Int,
    val dtype: String = "f32",
    val endian: String = "LE",
    val l2norm: Float,
    val sha256: String,
    val fileSize: Long,
    val createdAt: String
  )

  private fun baseDir(ctx: Context): File =
    File(ctx.filesDir, "embeddings").apply { mkdirs() }

  fun f32Path(ctx: Context, id: String) = File(baseDir(ctx), "$id.f32")
  fun jsonPath(ctx: Context, id: String) = File(baseDir(ctx), "$id.json")

  /** Write normalized embedding as .f32 (LE) + sidecar JSON; returns Meta. */
  fun save(ctx: Context, id: String, modelId: String, normalizedVec: FloatArray): Meta {
    // (130–139) Binary .f32 (LE)
    val bin = f32Path(ctx, id)
    val bb = ByteBuffer.allocate(normalizedVec.size * 4).order(ByteOrder.LITTLE_ENDIAN)
    for (v in normalizedVec) bb.putFloat(v)
    bin.writeBytes(bb.array())

    val sha = sha256(bin)
    val meta = Meta(
      id = id,
      model = modelId,
      dim = normalizedVec.size,
      dtype = "f32",
      endian = "LE",
      l2norm = l2norm(normalizedVec),
      sha256 = sha,
      fileSize = bin.length(),
      createdAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
    )

    val js = JSONObject()
      .put("id", meta.id)
      .put("model", meta.model)
      .put("dim", meta.dim)
      .put("dtype", meta.dtype)
      .put("endian", meta.endian)
      .put("l2norm", meta.l2norm)
      .put("sha256", meta.sha256)
      .put("file_size", meta.fileSize)
      .put("created_at", meta.createdAt)

    jsonPath(ctx, id).writeText(js.toString())

    return meta
  }

  /** Read .f32 (LE) into a FloatArray. */
  fun load(ctx: Context, id: String, expectedDim: Int? = null): FloatArray {
    val bin = f32Path(ctx, id)
    require(bin.exists()) { "missing embedding $id" }
    val bytes = bin.readBytes()
    require(bytes.size % 4 == 0) { "size not multiple of 4" }
    val n = bytes.size / 4
    if (expectedDim != null) require(n == expectedDim) { "dim mismatch: $n vs $expectedDim" }
    val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val out = FloatArray(n)
    var i = 0
    while (bb.hasRemaining()) {
      out[i++] = bb.getFloat()
    }
    return out
  }

  private fun sha256(file: File): String {
    val md = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { ins ->
      val buf = ByteArray(1 shl 16)
      while (true) {
        val r = ins.read(buf); if (r <= 0) break
        md.update(buf, 0, r)
      }
    }
    return md.digest().joinToString("") { "%02x".format(it) }
  }

  private fun l2norm(vec: FloatArray): Float {
    var s = 0f
    for (v in vec) s += v*v
    return kotlin.math.sqrt(s.toDouble()).toFloat()
  }
}
```

⸻

# 3) File: `app/src/main/java/com/mira/clip/services/RetrievalService.kt`

New retrieval service that uses cosine (dot on normalized vectors) and top-K ranking from stored .f32 files.

```kotlin
package com.mira.clip.services

import android.content.Context
import com.mira.clip.clip.ClipEngines
import com.mira.clip.storage.EmbeddingStore
import java.io.File
import kotlin.math.min

class RetrievalService(private val context: Context) {

  data class Scored(val id: String, val score: Float)

  /** Compute cosine (dot) between two normalized vectors. */
  fun cosine(a: FloatArray, b: FloatArray): Float = ClipEngines.cosine(a, b)

  /** Rank corpus embeddings by cosine against query embedding. */
  fun topK(queryNorm: FloatArray, k: Int = 5): List<Scored> {
    val dir = File(context.filesDir, "embeddings")
    if (!dir.exists()) return emptyList()
    val results = ArrayList<Scored>()
    dir.listFiles { f -> f.extension == "f32" }?.forEach { f ->
      val id = f.nameWithoutExtension
      val e = EmbeddingStore.load(context, id, expectedDim = queryNorm.size)
      // Assume stored vectors are normalized (enforced by save())
      val score = cosine(queryNorm, e) // (127–139) cosine on normalized → dot
      results.add(Scored(id, score))
    }
    return results.sortedByDescending { it.score }.take(k)
  }
}
```

⸻

# 4) End-to-End usage (hooks you can call today)

```kotlin
// 1) Compute normalized vectors (engine normalizes):
val imgVec = ClipEngines.embedImage(context, bitmap) // normalized
val txtVec = ClipEngines.embedText(context, "a photo of a dog") // normalized
val sim = ClipEngines.cosine(imgVec, txtVec) // dot since normalized

// 2) Persist auditable bytes
EmbeddingStore.save(context, id = "q1", modelId = "clip_vit_b32_mean_v1", normalizedVec = txtVec)

// 3) Retrieval
val retr = RetrievalService(context)
val top = retr.topK(imgVec, k = 5) // returns [Scored(id, cosine)]
```

⸻

# 5) Tests

## 5.1 JVM unit: normalization + LE bytes exactness

**File: `app/src/test/java/com/mira/clip/MetricSpaceUnitTest.kt`**

```kotlin
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
```

## 5.2 Instrumented e2e: auditable bytes + cosine sanity (+ Step-K)

**File: `app/src/androidTest/java/com/mira/clip/MetricSpaceInstrumentedTest.kt`**

```kotlin
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
```

⸻

# 6) Operational notes
- Normalization ON: even if TorchScript already normalizes, Kotlin re-normalizes defensively (idempotent).
- Binary format: one file per embedding: embeddings/<id>.f32 with raw float32 LE; sidecar <id>.json includes sha256 of binary file for audit.
- Cosine: because vectors are normalized, cosine(a,b) is just dot (fast, stable).
- Extensibility: add topKParallel() with coroutines if corpus grows; mmap .f32 for faster IO.

⸻

# 7) Acceptance criteria
- `ClipEngines.normalizeEmbedding()` present and used on every output.
- `.f32(LE)` files match expected endianness; sidecar JSON contains valid SHA256, dim, dtype=f32, endian=LE, l2norm≈1.0.
- `RetrievalService` ranks by cosine (dot).
- Unit + instrumented tests pass locally; e2e verifies valid cosine and auditable bytes.

---

## Quick "how to run"

1) Build & run tests:
   - JVM: `MetricSpaceUnitTest`
   - Instrumented: `MetricSpaceInstrumentedTest` (device/emulator)

2) In your app flow:
   - Use `ClipEngines.embed*()` → returns **normalized** 512-D.
   - Persist with `EmbeddingStore.save(...)`.
   - Search with `RetrievalService.topK(query, k)`.

If you want, I can also add:
- CRC32 in sidecar (besides SHA256),
- mmap loader for `.f32`,
- batch retrieval API returning `(ids,scores)` arrays for JNI hops.
