package com.mira.com.feature.retrieval
import kotlin.test.Test
import kotlin.test.assertTrue

class RetrievalTest {
  @Test fun cosineTopK() {
    val q = floatArrayOf(1f,0f,0f,0f)
    val a = Embedding("a", floatArrayOf(1f,0f,0f,0f))
    val b = Embedding("b", floatArrayOf(0f,1f,0f,0f))
    listOf(a,b).forEach { RetrievalMath.l2(it.vec) }
    RetrievalMath.l2(q)
    val top = EmbeddingStore.topK(q, listOf(a,b), 1).first().first.id
    assertTrue(top == "a")
  }
}
