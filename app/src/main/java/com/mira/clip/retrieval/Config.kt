package com.mira.clip.retrieval

import android.content.Context
import com.mira.storage.adapters.ClipStorageAdapter
import com.mira.storage.adapters.FaissStorageAdapter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class PipelineManifest(
  val variant: String = "clip_vit_b32",
  @SerialName("frame_count") val frameCount: Int = 32,
  @SerialName("batch_size")  val batchSize: Int = 8,
  val index: IndexCfg = IndexCfg(),
  val ingest: IngestCfg = IngestCfg(),
  val query: QueryCfg = QueryCfg()
) {
  @Serializable
  data class IndexCfg(
    val dir: String = "",  // Will be resolved dynamically using FaissStorageAdapter
    val type: String = "FLAT",            // FLAT | FAISS_IVFPQ | HNSW
    val nlist: Int = 4096,
    @SerialName("pq_m") val pqM: Int = 16,
    val nprobe: Int = 16
  ) {
    fun resolveDir(context: Context): String {
      return if (dir.isEmpty()) {
        FaissStorageAdapter.getVariantIndexDir(context, "clip_vit_b32").absolutePath
      } else {
        dir
      }
    }
  }
  
  @Serializable
  data class IngestCfg(
    val videos: List<String> = emptyList(),  // paths to .mp4; or leave empty if precomputed
    @SerialName("output_dir") val outputDir: String = ""  // Will be resolved dynamically
  ) {
    fun resolveOutputDir(context: Context): String {
      return if (outputDir.isEmpty()) {
        ClipStorageAdapter.getVariantEmbeddingsDir(context, "clip_vit_b32").absolutePath
      } else {
        outputDir
      }
    }
  }
  
  @Serializable
  data class QueryCfg(
    @SerialName("query_vec_path") val queryVecPath: String = "",  // Will be resolved dynamically
    @SerialName("top_k") val topK: Int = 50,
    @SerialName("output_path") val outputPath: String = ""  // Will be resolved dynamically
  ) {
    fun resolveQueryVecPath(context: Context): String {
      return if (queryVecPath.isEmpty()) {
        FaissStorageAdapter.getQueryVectorFile(context, "query").absolutePath
      } else {
        queryVecPath
      }
    }
    
    fun resolveOutputPath(context: Context): String {
      return if (outputPath.isEmpty()) {
        FaissStorageAdapter.getResultsFile(context, "q1").absolutePath
      } else {
        outputPath
      }
    }
  }
}

private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

fun loadManifest(path: String): PipelineManifest =
  json.decodeFromString(PipelineManifest.serializer(), File(path).readText())
