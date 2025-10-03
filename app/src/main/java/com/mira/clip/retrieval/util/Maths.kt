package com.mira.clip.retrieval.util
import kotlin.math.sqrt

object Maths {
  fun l2Normalize(x: FloatArray): FloatArray {
    var s = 0f; for (v in x) s += v*v
    val inv = if (s > 0f) (1f / sqrt(s)) else 0f
    return FloatArray(x.size) { i -> x[i] * inv }
  }
  fun cosineNormalized(qNorm: FloatArray, v: FloatArray): Float {
    val vNorm = l2Normalize(v)
    var dot = 0f
    for (i in qNorm.indices) dot += qNorm[i] * vNorm[i]
    return dot
  }
}
