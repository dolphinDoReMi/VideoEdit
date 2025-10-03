package com.mira.com.feature.clip

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri

object VideoFrameExtractor {
  data class Result(val durationUs: Long, val frames: List<Bitmap>, val timestampsUs: List<Long>)

  fun extractAt(ctx: Context, uri: Uri, timestampsUs: List<Long>, maxW: Int? = null, maxH: Int? = null): Result {
    val mmr = MediaMetadataRetriever()
    try {
      mmr.setDataSource(ctx, uri)
      val durMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
      val frames = ArrayList<Bitmap>(timestampsUs.size)
      for (t in timestampsUs) {
        val bm = mmr.getFrameAtTime(t, MediaMetadataRetriever.OPTION_CLOSEST) ?: continue
        val scaled = if (maxW != null && maxH != null && (bm.width > maxW || bm.height > maxH))
          Bitmap.createScaledBitmap(bm, maxW, maxH, true) else bm
        frames.add(scaled)
      }
      return Result(durMs * 1000, frames, timestampsUs)
    } finally { mmr.release() }
  }
}
