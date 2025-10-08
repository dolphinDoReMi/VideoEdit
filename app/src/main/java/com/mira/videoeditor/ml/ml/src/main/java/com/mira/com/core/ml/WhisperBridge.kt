package com.mira.com.core.ml

object WhisperBridge {
    init {
        System.loadLibrary("whisper_jni")
    }

    external fun decode(
        pcm16: ShortArray,
        sampleRate: Int,
        threads: Int,
    ): String
}
