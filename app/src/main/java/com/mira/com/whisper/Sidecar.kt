package com.mira.com.whisper

import android.content.Context
import org.json.JSONObject
import java.io.File

data class RunSnapshot(
    // Rails (UI defaults, read-only)
    val segmentMs: Int = 30000,
    val overlapMs: Int = 1000,
    val timestampPolicy: String = "ExtractorPTS",
    val decodePath: String = "mp4_aac_hw_first",
    val decodePolicy: String = "greedy",

    // From sidecar (nullable)
    val jobId: String? = null,
    val modelVariant: String? = null,
    val modelSha256: String? = null,
    val audioSha256: String? = null,
    val decodeCfg: String? = null,        // serialized or summary
    val configSha256: String? = null,
    val createdIso: String? = null,
    val inferMs: Long? = null,
    val audioMs: Long? = null,
    val rtf: Double? = null               // if null, compute from inferMs/audioMs
) {
    val effectiveRtf: Double? = when {
        rtf != null -> rtf
        inferMs != null && audioMs != null && audioMs > 0 -> inferMs.toDouble() / audioMs.toDouble()
        else -> null
    }
}

fun parseSidecarJson(json: String): RunSnapshot {
    val o = JSONObject(json)

    val jobId = o.optString("job_id", null)
    val modelVariant = o.optString("model_variant", null)
    val modelSha = o.optString("model_sha256", null)
    val audioSha = o.optString("audio_sha256", null)
    val configSha = o.optString("config_sha256", null)
    val created = o.optString("created", o.optString("created_at", null))

    val inferMs = when {
        o.has("infer_ms") -> o.optLong("infer_ms")
        else -> null
    }
    val audioMs = when {
        o.has("audio_ms") -> o.optLong("audio_ms")
        else -> null
    }
    val rtf = when {
        o.has("rtf") -> o.optDouble("rtf")
        else -> null
    }

    // decode_cfg can be an object or string; store a compact 1-line summary
    val decodeCfgSummary = when {
        o.has("decode_cfg") && o.opt("decode_cfg") is JSONObject -> {
            val d = o.getJSONObject("decode_cfg")
            val greedy = d.optBoolean("greedy", true)
            val beam = d.optInt("beam", 1)
            val temp = d.optDouble("temperature", 0.0)
            "greedy=$greedy, beam=$beam, temp=$temp"
        }
        o.has("decode_cfg") -> o.opt("decode_cfg").toString()
        else -> null
    }

    return RunSnapshot(
        jobId = jobId,
        modelVariant = modelVariant,
        modelSha256 = modelSha,
        audioSha256 = audioSha,
        decodeCfg = decodeCfgSummary,
        configSha256 = configSha,
        createdIso = created,
        inferMs = inferMs,
        audioMs = audioMs,
        rtf = rtf
    )
}

/**
 * Load sidecar from the canonical in-app path:
 *   <app>/files/whisper/{jobId}/sidecar.json
 * If not found and you pass exportDir=/sdcard/MiraWhisper/out, we'll also try:
 *   {exportDir}/{jobId}/sidecar.json
 */
fun loadSidecarForJob(
    context: Context,
    jobId: String,
    exportDir: File? = null
): String? {
    val inApp = File(context.filesDir, "whisper/$jobId/sidecar.json")
    if (inApp.exists()) return inApp.readText()

    if (exportDir != null) {
        val onSd = File(exportDir, "$jobId/sidecar.json")
        if (onSd.exists()) return onSd.readText()
    }
    return null
}
