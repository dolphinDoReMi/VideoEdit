package com.mira.clip.retrieval.io

import com.mira.clip.retrieval.index.SearchHit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

object ResultsWriter {
  @Serializable
  data class Results(val ids: List<String>, val scores: List<Float>)

  fun writeJson(path: String, hits: List<SearchHit>) {
    val r = Results(hits.map { it.id }, hits.map { it.score })
    File(path).writeText(Json { prettyPrint = true }.encodeToString(Results.serializer(), r))
  }
}
