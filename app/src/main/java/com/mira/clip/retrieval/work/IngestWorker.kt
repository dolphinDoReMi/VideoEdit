package com.mira.clip.retrieval.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mira.clip.retrieval.Actions
import com.mira.clip.retrieval.loadManifest
import com.mira.clip.retrieval.io.EmbeddingStore
import com.mira.clip.retrieval.util.FileIO.ensureDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class IngestWorker(ctx: Context, params: WorkerParameters)
  : CoroutineWorker(ctx, params) {

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    val manifestPath = inputData.getString(Actions.EXTRA_MANIFEST_PATH) ?: return@withContext Result.failure()
    val cfg = loadManifest(manifestPath)

    ensureDir(cfg.ingest.outputDir)

    // Try to obtain an embedder from your CLIP module (optional).
    val embedder = tryLoadClipEmbedder(cfg.variant)

    var processed = 0
    for (videoPath in cfg.ingest.videos) {
      val id = File(videoPath).nameWithoutExtension
      val vecOut = "${cfg.ingest.outputDir}/$id.f32"
      val metaOut = "${cfg.ingest.outputDir}/$id.json"

      val vec: FloatArray = when {
        embedder != null -> embedder.embedVideo(videoPath, cfg.frameCount, cfg.batchSize)
        else -> {
          // If no embedder is available, skip with failure for this item.
          // Alternatively, you can accept precomputed .f32 located beside video.
          return@withContext Result.failure()
        }
      }

      EmbeddingStore.writeVector(vecOut, vec)
      EmbeddingStore.writeMetadata(metaOut, id = id, source = videoPath, dim = vec.size,
        frameCount = cfg.frameCount, variant = cfg.variant)

      processed++
    }

    // Build/refresh index artifacts as needed (FLAT backend uses directory scan)
    ensureDir(cfg.index.dir)
    // Optional: write a marker file to bind this embedding root with index dir
    File("${cfg.index.dir}/BIND.txt").writeText(cfg.ingest.outputDir)

    Result.success()
  }
}

// Reflection bridge to your CLIP module (avoid hard dependency)
private interface ClipEmbedder {
  fun embedVideo(path: String, frameCount: Int, batchSize: Int): FloatArray
}
private fun tryLoadClipEmbedder(variant: String): ClipEmbedder? {
  return try {
    val clazz = Class.forName("com.mira.clip.engine.ClipEngines")
    val method = clazz.getMethod("embedVideo", String::class.java, Int::class.java, Int::class.java, String::class.java)
    object : ClipEmbedder {
      override fun embedVideo(path: String, frameCount: Int, batchSize: Int): FloatArray {
        val arr = method.invoke(null, path, frameCount, batchSize, variant) as FloatArray
        return arr
      }
    }
  } catch (_: Throwable) { null }
}
