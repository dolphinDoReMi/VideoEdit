package com.mira.com.feature.clip

data class FrameTimestamp(val presentationUs: Long)

object FrameSampler {
  fun uniform(durationUs: Long, frameCount: Int): List<FrameTimestamp> {
    if (frameCount <= 1) return listOf(FrameTimestamp(0L))
    
    val step = durationUs / (frameCount - 1)
    return (0 until frameCount).map { i ->
      FrameTimestamp(i * step)
    }
  }
}
