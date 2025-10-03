package com.mira.com.feature.retrieval
import java.nio.*
import java.nio.channels.FileChannel
import java.io.File
import kotlin.math.sqrt

data class Embedding(val id: String, val vec: FloatArray, val meta: Map<String, Any?> = emptyMap())

object RetrievalMath {
  fun l2(v: FloatArray) {
    var s = 0.0; for (x in v) s += x * x
    val inv = (1.0 / sqrt(s)).toFloat()
    for (i in v.indices) v[i] *= inv
  }
  fun cosine(a: FloatArray, b: FloatArray): Float { var s=0f; for (i in a.indices) s += a[i]*b[i]; return s }
}

object EmbeddingStore {
  fun writeAll(dim: Int, items: List<Embedding>, bin: File, meta: File) {
    FileChannel.open(bin.toPath(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE,
      java.nio.file.StandardOpenOption.TRUNCATE_EXISTING).use { ch ->
      val bb = ByteBuffer.allocate(items.size * dim * 4).order(ByteOrder.LITTLE_ENDIAN)
      for (e in items) for (f in e.vec) bb.putFloat(f)
      bb.flip(); ch.write(bb)
    }
    meta.writeText(items.joinToString(separator = "\n") { it.id }) // simple sidecar; replace w/ JSON as needed
  }
  fun readAll(dim: Int, bin: File, meta: File): List<Embedding> {
    val ids = if (meta.exists()) meta.readLines() else emptyList()
    FileChannel.open(bin.toPath(), java.nio.file.StandardOpenOption.READ).use { ch ->
      val size = ch.size().toInt()
      val bb = ch.map(FileChannel.MapMode.READ_ONLY, 0, size.toLong()).order(ByteOrder.LITTLE_ENDIAN)
      val n = size / (4 * dim)
      return List(n) { i ->
        val row = FloatArray(dim) { bb.float }
        Embedding(id = ids.getOrNull(i) ?: "item_$i", vec = row)
      }
    }
  }
  fun topK(query: FloatArray, items: List<Embedding>, k: Int): List<Pair<Embedding, Float>> {
    RetrievalMath.l2(query)
    return items.map { it to RetrievalMath.cosine(query, it.vec) }.sortedByDescending { it.second }.take(k)
  }
}
