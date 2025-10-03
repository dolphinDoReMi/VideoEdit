package com.mira.clip.retrieval.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mira.clip.retrieval.Actions
import com.mira.clip.retrieval.loadManifest
import com.mira.clip.retrieval.index.FlatIndexBackend
import com.mira.clip.retrieval.index.IndexBackend
import com.mira.clip.retrieval.io.EmbeddingStore
import com.mira.clip.retrieval.io.ResultsWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class RetrieveWorker(ctx: Context, params: WorkerParameters)
  : CoroutineWorker(ctx, params) {

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    val manifestPath = inputData.getString(Actions.EXTRA_MANIFEST_PATH) ?: return@withContext Result.failure()
    val cfg = loadManifest(manifestPath)

    // Resolve embedding root bound during ingest
    val bind = File("${cfg.index.dir}/BIND.txt").takeIf { it.exists() }?.readText()
      ?: cfg.ingest.outputDir // fallback

    val backend: IndexBackend = when (cfg.index.type.uppercase()) {
      "FLAT" -> FlatIndexBackend(bind)
      // "FAISS_IVFPQ" -> FaissIndexBackend(cfg.index) // implement via JNI later
      else   -> FlatIndexBackend(bind)
    }

    val q = EmbeddingStore.readVector(cfg.query.queryVecPath)
    val results = backend.searchCosineTopK(q, cfg.query.topK)

    ResultsWriter.writeJson(cfg.query.outputPath, results)
    Result.success()
  }
}
