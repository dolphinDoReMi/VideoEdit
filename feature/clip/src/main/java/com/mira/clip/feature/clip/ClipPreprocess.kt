package com.mira.com.feature.clip
import android.graphics.*

object ClipPreprocess {
  private val MEAN = floatArrayOf(0.48145466f, 0.4578275f, 0.40821073f)
  private val STD  = floatArrayOf(0.26862954f, 0.26130258f, 0.27577711f)

  fun centerCropResize(bm: Bitmap, size: Int): Bitmap {
    val minSide = minOf(bm.width, bm.height)
    val x = (bm.width - minSide) / 2
    val y = (bm.height - minSide) / 2
    val cropped = Bitmap.createBitmap(bm, x, y, minSide, minSide)
    return Bitmap.createScaledBitmap(cropped, size, size, true)
  }

  /** CHW float32 normalized for CLIP */
  fun toCHWFloat(bm: Bitmap): FloatArray {
    val w = bm.width; val h = bm.height; val n = w*h
    val out = FloatArray(3*n); val px = IntArray(n)
    bm.getPixels(px, 0, w, 0, 0, w, h)
    var rI = 0; var gI = n; var bI = 2*n
    for (i in 0 until n) {
      val p = px[i]
      val r = ((p ushr 16) and 0xFF) / 255f
      val g = ((p ushr 8) and 0xFF) / 255f
      val b = (p and 0xFF) / 255f
      out[rI++] = (r - MEAN[0]) / STD[0]
      out[gI++] = (g - MEAN[1]) / STD[1]
      out[bI++] = (b - MEAN[2]) / STD[2]
    }
    return out
  }
}
