package com.mira.clip.util

import android.content.Context
import com.google.gson.Gson
import java.io.File

object SamplerIo {
    fun appOutDir(context: Context, sub: String) =
        File(context.getExternalFilesDir(null), sub)

    fun writeJson(file: File, any: Any) {
        file.parentFile?.mkdirs()
        file.writeText(Gson().toJson(any))
    }
}
