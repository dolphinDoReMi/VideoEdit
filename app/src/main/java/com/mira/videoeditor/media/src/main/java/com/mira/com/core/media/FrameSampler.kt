package com.mira.com.core.media
data class FrameSpec(val presentationUs: Long)
object FrameSampler {
  fun uniform(durationUs: Long, count: Int): List<FrameSpec> {
    require(count >= 2)
    val step = durationUs.toDouble() / (count - 1)
    return (0 until count).map { i -> FrameSpec((i * step).toLong()) }
  }
}
