package com.mira.clip.retrieval

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
    val dir: String = "/sdcard/Mira/index/clip_vit_b32",
    val type: String = "FLAT",            // FLAT | FAISS_IVFPQ | HNSW
    val nlist: Int = 4096,
    @SerialName("pq_m") val pqM: Int = 16,
    val nprobe: Int = 16
  )
  @Serializable
  data class IngestCfg(
    val videos: List<String> = emptyList(),  // paths to .mp4; or leave empty if precomputed
    @SerialName("output_dir") val outputDir: String =
      "/sdcard/Mira/out/embeddings/clip_vit_b32"
  )
  @Serializable
  data class QueryCfg(
    @SerialName("query_vec_path") val queryVecPath: String =
      "/sdcard/Mira/query/query.f32",
    @SerialName("top_k") val topK: Int = 50,
    @SerialName("output_path") val outputPath: String =
      "/sdcard/Mira/out/results/q1.json"
  )
}

private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

fun loadManifest(path: String): PipelineManifest =
  json.decodeFromString(PipelineManifest.serializer(), File(path).readText())
