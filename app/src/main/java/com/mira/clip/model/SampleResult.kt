package com.mira.clip.model

data class SampleResult(
    val requestId: String,
    val inputUri: String,
    val frameCountExpected: Int,
    val frameCountObserved: Int,
    val timestampsMs: List<Long>,
    val durationMs: Long,
    val frames: List<String> // absolute paths
)
