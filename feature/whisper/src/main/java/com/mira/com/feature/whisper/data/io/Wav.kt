package com.mira.com.feature.whisper.data.io

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class WavPCM(val sr: Int, val ch: Int, val pcm16: ShortArray, val durationMs: Long)

object Wav {
    fun parse(le: ByteArray): WavPCM {
        val bb = ByteBuffer.wrap(le).order(ByteOrder.LITTLE_ENDIAN)

        fun readStr(n: Int) = ByteArray(n).also { bb.get(it) }.toString(Charsets.US_ASCII)
        val riff = readStr(4)
        bb.int
        val wave = readStr(4)
        require(riff == "RIFF" && wave == "WAVE") { "Bad WAV header" }
        var fmtFound = false
        var dataFound = false
        var sr = 0
        var ch = 0
        var bits = 0
        var dataOffset = 0
        var dataLen = 0
        while (bb.remaining() >= 8) {
            val id = readStr(4)
            val sz = bb.int
            if (id == "fmt ") {
                fmtFound = true
                val audioFmt = bb.short.toInt() and 0xFFFF
                ch = bb.short.toInt() and 0xFFFF
                sr = bb.int
                bb.int // byteRate
                bb.short // blockAlign
                bits = bb.short.toInt() and 0xFFFF
                bb.position(bb.position() + (sz - 16).coerceAtLeast(0))
                require(audioFmt == 1 && bits == 16) { "PCM16 only" }
            } else if (id == "data") {
                dataFound = true
                dataOffset = bb.position()
                dataLen = sz
                bb.position(bb.position() + sz)
            } else {
                bb.position(bb.position() + sz)
            }
        }
        require(fmtFound && dataFound) { "fmt/data chunk missing" }
        val pcmBB = ByteBuffer.wrap(le, dataOffset, dataLen).order(ByteOrder.LITTLE_ENDIAN)
        val samples = dataLen / 2
        val pcm = ShortArray(samples)
        pcmBB.asShortBuffer().get(pcm)
        val frames = samples / ch
        val durMs = (frames * 1000L) / sr
        return WavPCM(sr, ch, pcm, durMs)
    }
}
