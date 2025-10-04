package com.mira.com.whisper.console.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WhisperSidecar(
    @Json(name = "job_id") val jobId: String? = null,
    @Json(name = "uri") val uri: String? = null,
    @Json(name = "audio_sha256") val audioSha256: String? = null,
    @Json(name = "model_sha256") val modelSha256: String? = null,
    @Json(name = "transcript_sha256") val transcriptSha256: String? = null,
    @Json(name = "infer_ms") val inferMs: Long? = null,
    @Json(name = "audio_ms") val audioMs: Long? = null,
    @Json(name = "durationMs") val durationMs: Long? = null,
    @Json(name = "started_at") val startedAt: Long? = null,
    @Json(name = "finished_at") val finishedAt: Long? = null,
    @Json(name = "config") val config: Map<String, Any?>? = null,
    @Json(name = "error") val error: String? = null
)

enum class JobStatus { RUNNING, COMPLETED, ERROR, TIMEOUT }

data class JobRow(
    val jobId: String,
    val baseName: String,
    val rtf: Double?,            // infer_ms / audio_ms
    val status: JobStatus,
    val audioSha256: String?,
    val modelSha256: String?,
    val transcriptSha256: String?,
    val durationMs: Long?,
    val startedAt: Long?,
    val finishedAt: Long?,
    val config: Map<String, Any?>?,
    val dirPath: String
)
