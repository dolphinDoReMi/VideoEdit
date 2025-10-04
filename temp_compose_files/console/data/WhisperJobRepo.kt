package com.mira.com.whisper.console.data

import android.os.FileObserver
import com.mira.com.whisper.console.model.*
import com.squareup.moshi.Moshi
import okio.buffer
import okio.source
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.round

class WhisperJobRepo(
    private val baseOutDir: File = File("/sdcard/MiraWhisper/out"),
    private val timeoutMinutes: Long = 10L,    // N minutes threshold
    private val moshi: Moshi = Moshi.Builder().build()
) {
    private val sidecarAdapter = moshi.adapter(WhisperSidecar::class.java)

    fun listJobs(): List<JobRow> {
        if (!baseOutDir.exists()) return emptyList()
        val now = System.currentTimeMillis()
        return baseOutDir.listFiles { f -> f.isDirectory }?.sortedByDescending { it.lastModified() }
            ?.mapNotNull { jobDir ->
                val parsed = readFirstSidecar(jobDir) ?: return@mapNotNull null
                val sc = parsed.first
                val chosen = parsed.second
                val rtf = computeRtf(sc.inferMs, sc.audioMs)
                val status = deriveStatus(sc, jobDir, now)
                val baseName = basenameFrom(sc.uri) ?: jobDir.name
                JobRow(
                    jobId = sc.jobId ?: jobDir.name,
                    baseName = baseName,
                    rtf = rtf,
                    status = status,
                    audioSha256 = sc.audioSha256,
                    modelSha256 = sc.modelSha256,
                    transcriptSha256 = sc.transcriptSha256,
                    durationMs = sc.durationMs ?: sc.audioMs,
                    startedAt = sc.startedAt,
                    finishedAt = sc.finishedAt,
                    config = sc.config,
                    dirPath = jobDir.absolutePath
                )
            } ?: emptyList()
    }

    private fun readFirstSidecar(jobDir: File): Pair<WhisperSidecar, File>? {
        val candidates = listOf("artifact.json", "transcript.json") +
                (jobDir.listFiles { f -> f.isFile && f.extension == "json" }?.map { it.name } ?: emptyList())
        val seen = HashSet<String>()
        for (name in candidates) {
            if (!seen.add(name)) continue
            val f = File(jobDir, name)
            if (!f.exists() || !f.isFile) continue
            runCatching {
                f.source().buffer().use { buf ->
                    val sc = sidecarAdapter.fromJson(buf) ?: return@runCatching null
                    return sc to f
                }
            }.onFailure {
                // tolerate parse failure; try next candidate
            }
        }
        return null
    }

    private fun computeRtf(inferMs: Long?, audioMs: Long?): Double? {
        if (inferMs == null || audioMs == null || audioMs <= 0) return null
        return inferMs.toDouble() / audioMs.toDouble()
    }

    private fun deriveStatus(sc: WhisperSidecar, jobDir: File, nowMs: Long): JobStatus {
        if (!sc.error.isNullOrBlank()) return JobStatus.ERROR
        if (!sc.transcriptSha256.isNullOrBlank()) return JobStatus.COMPLETED
        val ageMin = TimeUnit.MILLISECONDS.toMinutes(nowMs - maxOf(jobDir.lastModified(), sc.finishedAt ?: 0))
        if (ageMin > timeoutMinutes) return JobStatus.TIMEOUT
        return JobStatus.RUNNING
    }

    private fun basenameFrom(uri: String?): String? {
        if (uri.isNullOrBlank()) return null
        val last = uri.substringAfterLast('/').substringAfterLast('%').substringAfterLast(':')
        return last.ifBlank { null }
    }

    // ---------- Watching ----------
    private var observer: FileObserver? = null
    private val _events = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun watchJobs(): Flow<Unit> {
        if (!baseOutDir.exists()) baseOutDir.mkdirs()
        observer?.stopWatching()
        observer = object : FileObserver(baseOutDir.absolutePath, CREATE or MOVED_TO or CLOSE_WRITE or MODIFY or DELETE) {
            override fun onEvent(event: Int, path: String?) {
                _events.tryEmit(Unit)
            }
        }.also { it.startWatching() }

        // Debounce to limit refresh storms
        return events.debounce(350)
    }

    fun stopWatching() {
        observer?.stopWatching()
        observer = null
    }
}
