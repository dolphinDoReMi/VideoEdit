package com.mira.clip.debug

import android.os.Bundle
import android.widget.TextView
import android.widget.ScrollView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.mira.clip.clip.ClipEngines
import com.mira.clip.core.Config
import com.mira.clip.video.FrameSampler
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Foreground activity to generate a CLIP embedding for a video and persist it
 * under the app external files directory for easy adb access:
 *   /sdcard/Android/data/<package>/files/embeddings/<variant>/<videoId>.f32
 */
class DebugEmbeddingActivity : ComponentActivity() {
    companion object { private const val TAG = "DebugEmbeddingActivity" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = ScrollView(this)
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        val tv = TextView(this)
        tv.text = "Running CLIP embedding generator...\nThis may take a minute."
        container.addView(tv)
        root.addView(container)
        setContentView(root)

        val videoPath = intent.getStringExtra("video_path") ?: Config.DEFAULT_VIDEO_PATH
        val variant = intent.getStringExtra("variant") ?: Config.DEFAULT_VARIANT
        val frameCount = intent.getIntExtra("frame_count", Config.DEFAULT_FRAME_COUNT)

        Thread {
            try {
                // 1) Sample frames
                val frames = FrameSampler.sampleUniform(this, videoPath, frameCount)

                // 2) Initialize and encode
                val engines = ClipEngines(this)
                engines.initialize()
                val embedding = engines.encodeFrames(frames)

                // Log stats + full vector to logcat for adb capture
                val l2 = kotlin.math.sqrt(embedding.sumOf { (it * it).toDouble() })
                Log.i(TAG, "EMBED_DIM=${embedding.size} L2=${l2}")
                // Chunk logging to avoid truncation
                val chunkSize = 128
                var idx = 0
                while (idx < embedding.size) {
                    val end = (idx + chunkSize).coerceAtMost(embedding.size)
                    val chunk = embedding.slice(idx until end).joinToString(",") { it.toString() }
                    Log.i(TAG, "EMBED_VEC[${idx}-${end-1}]=[$chunk]")
                    idx = end
                }

                // 3) Persist to external files dir
                val extRoot = getExternalFilesDir(null) ?: filesDir
                val variantDir = File(extRoot, "embeddings/${variant}")
                variantDir.mkdirs()

                val videoId = File(videoPath).nameWithoutExtension.ifBlank { "video_v1" }

                val vecFile = File(variantDir, "$videoId.f32")
                val metaFile = File(variantDir, "$videoId.json")

                val bb = ByteBuffer.allocate(embedding.size * 4).order(ByteOrder.LITTLE_ENDIAN)
                for (v in embedding) bb.putFloat(v)
                vecFile.writeBytes(bb.array())

                val meta = JSONObject()
                    .put("video_id", videoId)
                    .put("video_path", videoPath)
                    .put("variant", variant)
                    .put("frame_count", frames.size)
                    .put("embedding_dimension", embedding.size)
                    .put("output_path", vecFile.absolutePath)
                    .put("timestamp", System.currentTimeMillis())
                metaFile.writeText(meta.toString(2))

                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Embedding saved: ${vecFile.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Embedding generation failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }.start()
    }
}


