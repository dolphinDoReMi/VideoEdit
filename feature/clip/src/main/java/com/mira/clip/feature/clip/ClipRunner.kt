package com.mira.com.feature.clip

import android.content.Context
import android.net.Uri
import android.util.Log
import com.mira.com.core.infra.Config
import com.mira.com.core.media.FrameSampler
import com.mira.com.feature.retrieval.Embedding
import com.mira.com.feature.retrieval.EmbeddingStore
import org.json.JSONObject
import java.io.File

object ClipRunner {
  private const val TAG = "ClipRunner"

  /**
   * Runs video -> frames -> preprocess -> encode -> mean-pool -> persist.
   * @param inputUriStr  e.g., file:///sdcard/Mira/video_v1.mp4
   * @param outDirStr    e.g., file:///sdcard/MiraClip/out
   * @param variant      logical model variant, e.g. "ViT-B/32"
   * @param frameCount   number of frames to sample (>=2)
   */
  fun run(ctx: Context, inputUriStr: String, outDirStr: String, variant: String, frameCount: Int): File {
    val uri = Uri.parse(inputUriStr)
    val outDir = File(Uri.parse(outDirStr).path ?: outDirStr).also { it.mkdirs() }

    // 1) Inspect duration & timestamps
    val probe = VideoFrameExtractor.extractAt(ctx, uri, listOf(0L)) // quick probe
    val durationUs = probe.durationUs
    val stamps = FrameSampler.uniform(durationUs, frameCount).map { it.presentationUs }

    // 2) Extract frames at timestamps
    val frames = VideoFrameExtractor.extractAt(ctx, uri, stamps, maxW = 512, maxH = 512).frames
    require(frames.isNotEmpty()) { "No frames decoded; input=$inputUriStr" }

    // 3) Preprocess & encode
    val embeds = frames.map { bm ->
      val resized = ClipPreprocess.centerCropResize(bm, Config.CLIP_RES)
      val chw = ClipPreprocess.toCHWFloat(resized)
      ClipEngine.encodeImageCHW(chw, 512)
    }

    // 4) Mean pool to video embedding
    val videoEmbedding = ClipEngine.meanPool(embeds)

    // 5) Persist store + JSON
    val videoId = File(uri.path ?: "video").nameWithoutExtension.ifBlank { "video" }
    val variantDir = File(outDir, "embeddings/${variant.replace('/', '_')}")
    variantDir.mkdirs()

    val bin = File(variantDir, "$videoId.f32")
    val meta = File(variantDir, "$videoId.json")

    EmbeddingStore.writeAll(
      dim = videoEmbedding.size,
      items = listOf(Embedding(id = videoId, vec = videoEmbedding)),
      bin = bin,
      meta = meta // will be overwritten below with structured JSON
    )

    meta.writeText(
      JSONObject()
        .put("id", videoId)
        .put("src", inputUriStr)
        .put("variant", variant)
        .put("dim", videoEmbedding.size)
        .put("frame_count", frames.size)
        .put("timestamps_us", stamps)
        .put("duration_us", durationUs)
        .toString()
    )
    Log.i(TAG, "Wrote ${bin.absolutePath}")
    return bin
  }
}
