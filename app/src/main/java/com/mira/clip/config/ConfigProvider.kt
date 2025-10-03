package com.mira.clip.config

import android.content.Context
import com.mira.clip.BuildConfig

object ConfigProvider {
    fun defaultConfig(context: Context): SamplerConfig {
        val fc = BuildConfig.DEFAULT_FRAME_COUNT
        val schedule = Schedule.valueOf(BuildConfig.DEFAULT_SCHEDULE)
        val backend = DecodeBackend.valueOf(BuildConfig.DEFAULT_DECODE_BACKEND)
        val mem = BuildConfig.DEFAULT_MEMORY_BUDGET_MB

        // Debug overrides via SharedPreferences (debug only UI writes these)
        val sp = context.getSharedPreferences("sampler_overrides", Context.MODE_PRIVATE)
        val frameCount = sp.getInt("frameCount", fc)
        val scheduleOv = sp.getString("schedule", schedule.name)!!
        val backendOv = sp.getString("decodeBackend", backend.name)!!
        val memoryMb = sp.getInt("memoryBudgetMb", mem)

        return SamplerConfig(
            frameCount = frameCount,
            schedule = Schedule.valueOf(scheduleOv),
            decodeBackend = DecodeBackend.valueOf(backendOv),
            memoryBudgetMb = memoryMb
        )
    }
}
