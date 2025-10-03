package com.mira.com.core.media
import android.content.Context
import android.net.Uri

class MediaAudioDecoder {
    data class PCM(val sampleRate: Int, val channels: Int, val data: ShortArray)

    fun decodeToPcm16(
        input: Uri,
        ctx: Context,
    ): PCM {
        // For now, throw UnsupportedOperationException as this is a placeholder
        // TODO: Implement full AAC→PCM16 decode with MediaCodec for production.
        // WAV path should be handled separately in the feature module
        throw UnsupportedOperationException("Implement AAC decode with MediaCodec or handle WAV fast path")
    }
}
