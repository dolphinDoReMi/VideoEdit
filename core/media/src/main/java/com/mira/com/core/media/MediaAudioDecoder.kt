package com.mira.com.core.media
import android.media.*
import androidx.media3.extractor.*
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.common.C
import java.nio.*

class MediaAudioDecoder {
  data class PCM(val sampleRate: Int, val channels: Int, val data: ShortArray)
  fun decodeToPcm16(input: android.net.Uri, ctx: android.content.Context): PCM {
    val resolver = ctx.contentResolver
    resolver.openInputStream(input).use { ins ->
      val extractor = DefaultExtractorInput( /* dataSource */ object: androidx.media3.common.DataReader {
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int = ins!!.read(buffer, offset, length)
      }, 0, C.LENGTH_UNSET.toLong())

      // For brevity, assume WAV path handled separately; otherwise build MediaCodec decode here.
      // TODO: Implement full AAC→PCM16 decode with MediaCodec for production.
      throw UnsupportedOperationException("Implement AAC decode with MediaCodec or handle WAV fast path")
    }
  }
}
