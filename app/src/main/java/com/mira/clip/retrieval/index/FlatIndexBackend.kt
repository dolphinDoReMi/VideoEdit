package com.mira.clip.retrieval.index

import com.mira.clip.retrieval.io.EmbeddingStore
import com.mira.clip.retrieval.util.Maths
import java.io.File
import java.util.PriorityQueue
import kotlin.math.min

class FlatIndexBackend(private val embeddingRoot: String) : IndexBackend {
  override fun searchCosineTopK(query: FloatArray, k: Int): List<SearchHit> {
    val q = Maths.l2Normalize(query)
    val heap = PriorityQueue<Pair<String, Float>>(k, compareBy { it.second }) // min-heap by score

    File(embeddingRoot).listFiles { f -> f.extension == "f32" }?.forEach { f ->
      val id = f.nameWithoutExtension
      val v = EmbeddingStore.readVector(f.absolutePath)
      val score = Maths.cosineNormalized(q, v) // robust if v not normalized
      if (heap.size < k) heap.offer(id to score)
      else if (score > heap.peek()?.second ?: 0f) { heap.poll(); heap.offer(id to score) }
    }

    return heap.toList().sortedByDescending { it.second }
      .map { SearchHit(it.first, it.second) }
      .take(min(k, heap.size))
  }
}
