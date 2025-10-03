package com.mira.clip.retrieval.index

data class SearchHit(val id: String, val score: Float)

interface IndexBackend {
  fun searchCosineTopK(query: FloatArray, k: Int): List<SearchHit>
}
