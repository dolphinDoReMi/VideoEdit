package com.mira.com.feature.whisper.util

import com.mira.com.feature.whisper.data.db.AsrSegment
import com.mira.com.feature.whisper.engine.WhisperParams
import org.json.JSONArray
import org.json.JSONObject

object Sidecars {
    fun build(
        uri: String,
        durationMs: Long,
        params: WhisperParams,
        inferMs: Long,
        rtf: Double,
        segmentsJson: JSONArray?,
    ): JSONObject {
        val root =
            JSONObject()
                .put("version", "1.0")
                .put(
                    "audio",
                    JSONObject()
                        .put("uri", uri).put("sr_hz", 16000).put("channels", 1).put("duration_ms", durationMs),
                )
                .put(
                    "job",
                    JSONObject()
                        .put("model", params.model).put("threads", params.threads).put("beam", params.beam)
                        .put("lang", params.lang).put("translate", params.translate)
                        .put("rtf", rtf).put("infer_ms", inferMs),
                )
        val segs = JSONArray()
        if (segmentsJson != null) {
            for (i in 0 until segmentsJson.length()) {
                val s = segmentsJson.getJSONObject(i)
                segs.put(
                    JSONObject()
                        .put("t0_ms", (s.optDouble("t0", 0.0) * 1000).toLong())
                        .put("t1_ms", (s.optDouble("t1", 0.0) * 1000).toLong())
                        .put("text", s.optString("text")),
                )
            }
        }
        root.put("segments", segs)
        return root
    }

    fun segmentsFrom(
        sidecar: JSONObject,
        jobId: String,
    ): List<AsrSegment> {
        val arr = sidecar.getJSONArray("segments")
        return buildList {
            for (i in 0 until arr.length()) {
                val s = arr.getJSONObject(i)
                add(AsrSegment(jobId = jobId, t0Ms = s.getLong("t0_ms"), t1Ms = s.getLong("t1_ms"), text = s.getString("text")))
            }
        }
    }
}
