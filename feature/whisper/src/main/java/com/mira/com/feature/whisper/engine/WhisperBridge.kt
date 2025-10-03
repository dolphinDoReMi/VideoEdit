package com.mira.com.feature.whisper.engine

object WhisperBridge {
    init {
        System.loadLibrary("whisper_jni")
    }

    external fun decodeJson(
        pcm16: ShortArray,
        sampleRate: Int,
        modelPath: String,
        threads: Int,
        beam: Int,
        lang: String,
        translate: Boolean,
    ): String
}
