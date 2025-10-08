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
import com.mira.storage.ScopedStorageManager
import com.mira.storage.adapters.FaissStorageAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class RetrieveWorker(ctx: Context, params: WorkerParameters)
  : CoroutineWorker(ctx, params) {

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    val manifestPath = inputData.getString(Actions.EXTRA_MANIFEST_PATH) ?: return@withContext Result.failure()
    val cfg = loadManifest(manifestPath)

    // Ensure directory structure exists
    ScopedStorageManager.ensureDirectoryStructure(applicationContext)

    // Resolve embedding root bound during ingest
    val bindFile = FaissStorageAdapter.getBindFile(applicationContext, cfg.variant)
    val bind = if (bindFile.exists()) {
      bindFile.readText()
    } else {
      cfg.ingest.resolveOutputDir(applicationContext) // fallback
    }

    val backend: IndexBackend = when (cfg.index.type.uppercase()) {
      "FLAT" -> FlatIndexBackend(bind)
      // "FAISS_IVFPQ" -> FaissIndexBackend(cfg.index) // implement via JNI later
      else   -> FlatIndexBackend(bind)
    }

    val queryVecPath = cfg.query.resolveQueryVecPath(applicationContext)
    val q = EmbeddingStore.readVector(queryVecPath)
    val results = backend.searchCosineTopK(q, cfg.query.topK)

    val outputPath = cfg.query.resolveOutputPath(applicationContext)
    ResultsWriter.writeJson(outputPath, results)
    Result.success()
  }
}
