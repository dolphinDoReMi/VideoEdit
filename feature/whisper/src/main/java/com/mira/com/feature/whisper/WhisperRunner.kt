package com.mira.com.feature.whisper
import android.content.Context
import android.net.Uri
import android.util.Log
import com.mira.com.core.infra.Config
import com.mira.com.core.media.AudioResampler
import com.mira.com.core.ml.WhisperBridge
import org.json.JSONObject
import java.io.File

object WhisperRunner {
    fun run(
        ctx: Context,
        audio: Uri,
        outDir: File,
    ): File {
        // TODO: Proper AAC→PCM16 with MediaCodec; here assume WAV mono16k for brevity
        val wav = ctx.contentResolver.openInputStream(audio)!!.readBytes()
        // Placeholder: convert bytes to ShortArray if WAV; else decode with MediaCodec
        val pcm = ShortArray(wav.size / 2)
        java.nio.ByteBuffer.wrap(wav).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(pcm)

        val mono = AudioResampler.downmixToMono(pcm, 1)
        val sr16 = AudioResampler.resampleLinear(mono, 16_000, 16_000)
        val json = WhisperBridge.decode(sr16, 16_000, Config.WHISPER_THREADS)
        val f = File(outDir, "transcript.json")
        f.writeText(JSONObject(json).toString())
        Log.i("WhisperRunner", "Wrote ${f.absolutePath}")
        return f
    }
}
