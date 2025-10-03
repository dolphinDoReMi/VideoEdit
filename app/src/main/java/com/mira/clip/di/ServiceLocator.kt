package com.mira.clip.di

import android.content.Context
import com.mira.clip.usecases.ComputeClipSimilarityUseCase

object ServiceLocator {
  fun similarityUseCase(ctx: Context) = ComputeClipSimilarityUseCase(ctx)
}
