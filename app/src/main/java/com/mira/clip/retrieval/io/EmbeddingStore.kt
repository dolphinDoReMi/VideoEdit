package com.mira.clip.retrieval.io

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

object EmbeddingStore {
  fun writeVector(path: String, vec: FloatArray) {
    val bb = ByteBuffer.allocate(vec.size * 4).order(ByteOrder.LITTLE_ENDIAN)
    vec.forEach { bb.putFloat(it) }
    File(path).outputStream().use { it.write(bb.array()) }
  }
  fun readVector(path: String): FloatArray {
    val bytes = File(path).readBytes()
    val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val out = FloatArray(bytes.size / 4)
    var i = 0
    while (bb.hasRemaining()) out[i++] = bb.getFloat()
    return out
  }

  @Serializable
  data class Meta(val id: String, val source: String, val dim: Int, val frame_count: Int, val variant: String)
  private val json = Json { prettyPrint = true }
  fun writeMetadata(path: String, id: String, source: String, dim: Int, frameCount: Int, variant: String) {
    val m = Meta(id, source, dim, frameCount, variant)
    File(path).writeText(json.encodeToString(Meta.serializer(), m))
  }
}
