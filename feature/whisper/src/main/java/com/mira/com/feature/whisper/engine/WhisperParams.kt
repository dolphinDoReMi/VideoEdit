package com.mira.com.feature.whisper.engine

data class WhisperParams(
    val model: String,
    val threads: Int = 4,
    val beam: Int = 0,
    val lang: String = "auto",
    val translate: Boolean = false,
)
