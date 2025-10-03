package com.mira.com
import com.mira.com.feature.retrieval.*

object Smoke {
  fun run(): String {
    val q = FloatArray(4) { if (it == 0) 1f else 0f }
    val db = listOf(
      Embedding("a", floatArrayOf(1f,0f,0f,0f)),
      Embedding("b", floatArrayOf(0f,1f,0f,0f)),
      Embedding("c", floatArrayOf(0.7f,0.7f,0f,0f))
    )
    db.forEach { RetrievalMath.l2(it.vec) }
    RetrievalMath.l2(q)
    val top = EmbeddingStore.topK(q, db, 1).first()
    return "best=${top.first.id} score=%.3f".format(top.second)
  }
}
