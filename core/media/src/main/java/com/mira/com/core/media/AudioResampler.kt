package com.mira.com.core.media

object AudioResampler {
    // Downmix interleaved PCM16 to mono (mean)
    fun downmixToMono(
        src: ShortArray,
        channels: Int,
    ): ShortArray {
        if (channels == 1) return src
        val frames = src.size / channels
        val out = ShortArray(frames)
        var i = 0
        for (f in 0 until frames) {
            var acc = 0
            for (c in 0 until channels) acc += src[i++].toInt()
            out[f] = (acc / channels).toShort()
        }
        return out
    }

    // Linear resample to 16 kHz (fast; replace with band-limited if needed)
    fun resampleLinear(
        src: ShortArray,
        srcRate: Int,
        dstRate: Int = 16_000,
    ): ShortArray {
        if (srcRate == dstRate) return src
        val ratio = dstRate.toDouble() / srcRate
        val out = ShortArray((src.size * ratio).toInt())
        for (i in out.indices) {
            val x = i / ratio
            val x0 = x.toInt().coerceIn(0, src.size - 1)
            val x1 = (x0 + 1).coerceAtMost(src.size - 1)
            val t = x - x0
            val v = (src[x0] * (1 - t) + src[x1] * t).toInt()
            out[i] = v.toShort()
        }
        return out
    }
}
