package com.mira.com.core.infra

object Config {
    const val WHISPER_SR = 16_000
    const val WHISPER_CH = 1
    const val WHISPER_THREADS = 4
    const val WHISPER_BEAM = 0

    const val CLIP_RES = 224
    const val CLIP_FRAME_COUNT = 32
    const val CLIP_SCHEDULE = "uniform"
    const val CLIP_TEXT_MAX_TOKENS = 77

    const val RETR_USE_L2_NORM = true
    const val RETR_SIMILARITY = "cosine"
    const val RETR_STORAGE_FMT = ".f32"
    const val RETR_ENABLE_ANN = false
}
