package com.mira.clip.usecases

import android.content.Context
import android.graphics.Bitmap
import com.mira.clip.clip.ClipEngines

class ComputeClipSimilarityUseCase(private val context: Context) {
  fun run(image: Bitmap, text: String): Pair<FloatArray, FloatArray> {
    val iv = ClipEngines.embedImage(context, image)
    val tv = ClipEngines.embedText(context, text)
    return iv to tv
  }
  fun cosine(a: FloatArray, b: FloatArray) = ClipEngines.cosine(a,b)
}
