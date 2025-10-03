package com.mira.clip.debug.debugui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mira.clip.config.Schedule
import com.mira.clip.config.DecodeBackend

class SamplerDebugActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sp = getSharedPreferences("sampler_overrides", MODE_PRIVATE)
        // Minimal UI omitted — example of writing overrides:
        sp.edit()
            .putInt("frameCount", 32)
            .putString("schedule", Schedule.UNIFORM.name)
            .putString("decodeBackend", DecodeBackend.MMR.name)
            .apply()
        finish()
    }
}
