package com.mira.clip.util

import android.content.Intent

object SamplerIntents {
    const val ACTION_SAMPLE_REQUEST = "com.mira.clip.action.SAMPLE_REQUEST"
    const val ACTION_SAMPLE_PROGRESS = "com.mira.clip.action.SAMPLE_PROGRESS"
    const val ACTION_SAMPLE_RESULT = "com.mira.clip.action.SAMPLE_RESULT"
    const val ACTION_SAMPLE_ERROR = "com.mira.clip.action.SAMPLE_ERROR"

    const val EXTRA_INPUT_URI = "com.mira.clip.extra.INPUT_URI"
    const val EXTRA_REQUEST_ID = "com.mira.clip.extra.REQUEST_ID"
    const val EXTRA_FRAME_COUNT = "com.mira.clip.extra.FRAME_COUNT"
    const val EXTRA_PROGRESS = "com.mira.clip.extra.PROGRESS"
    const val EXTRA_RESULT_JSON = "com.mira.clip.extra.RESULT_JSON"
    const val EXTRA_ERROR_MESSAGE = "com.mira.clip.extra.ERROR_MESSAGE"

    fun forPackage(intent: Intent, pkg: String) = intent.setPackage(pkg)
}
