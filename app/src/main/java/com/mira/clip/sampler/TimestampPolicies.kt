package com.mira.clip.sampler

import kotlin.math.roundToLong
import java.util.Random

object TimestampPolicies {

    fun uniform(durationMs: Long, n: Int): LongArray {
        require(n >= 2) { "n>=2" }
        if (durationMs <= 0) return LongArray(n) { 0 }
        val stamps = LongArray(n)
        val denom = (n - 1).toDouble()
        for (i in 0 until n) {
            val t = (i / denom) * durationMs.toDouble()
            stamps[i] = t.roundToLong().coerceIn(0, durationMs)
            if (i > 0 && stamps[i] <= stamps[i - 1]) stamps[i] = (stamps[i - 1] + 1).coerceAtMost(durationMs)
        }
        return stamps
    }

    fun tsnJitter(durationMs: Long, n: Int, rng: Random = Random(42)): LongArray {
        require(n >= 2)
        if (durationMs <= 0) return LongArray(n) { 0 }
        val seg = durationMs.toDouble() / n
        val stamps = LongArray(n)
        for (i in 0 until n) {
            val start = i * seg
            val end = (i + 1) * seg
            val jitter = start + rng.nextDouble() * (end - start)
            stamps[i] = jitter.roundToLong().coerceIn(0, durationMs)
            if (i > 0 && stamps[i] <= stamps[i - 1]) stamps[i] = (stamps[i - 1] + 1).coerceAtMost(durationMs)
        }
        return stamps
    }
}
