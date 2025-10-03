package com.mira.com.feature.clip
import com.mira.com.core.infra.Config
import kotlin.math.sqrt

object ClipEngine {
  /** Stub encoder; replace with Torch/NNAPI later. Output dim stays stable (e.g., 512). */
  fun encodeImageCHW(chw: FloatArray, dim: Int = 512): FloatArray {
    val out = FloatArray(dim)
    for (i in chw.indices) out[i % dim] += chw[i]
    var s = 0.0; for (x in out) s += x*x
    val inv = if (s > 0) (1.0 / sqrt(s)).toFloat() else 1f
    for (i in out.indices) out[i] *= inv
    return out
  }
  fun meanPool(frames: List<FloatArray>): FloatArray {
    val d = frames.first().size; val out = FloatArray(d)
    for (f in frames) for (i in 0 until d) out[i] += f[i]
    for (i in 0 until d) out[i] /= frames.size
    return out
  }
}
