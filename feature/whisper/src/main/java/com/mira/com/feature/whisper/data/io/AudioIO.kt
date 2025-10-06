package com.mira.com.feature.whisper.data.io

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import java.nio.ByteBuffer
import kotlin.math.max

data class PCM(val sr: Int, val ch: Int, val pcm16: ShortArray, val durationMs: Long)

object AudioIO {
    fun loadPcm16(
        ctx: Context,
        uri: Uri,
    ): PCM {
        val path = uri.toString().lowercase()
        return if (path.endsWith(".wav")) {
            val bytes = ctx.contentResolver.openInputStream(uri)!!.readBytes()
            val w = Wav.parse(bytes)
            PCM(w.sr, w.ch, w.pcm16, w.durationMs)
        } else {
            decodeAacMp4(ctx, uri)
        }
    }

    private fun decodeAacMp4(
        ctx: Context,
        uri: Uri,
    ): PCM {
        val extractor = MediaExtractor()
        extractor.setDataSource(ctx, uri, null)
        var aTrack = -1
        var fmt: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                aTrack = i
                fmt = f
                break
            }
        }
        require(aTrack >= 0 && fmt != null) { "No audio track" }
        extractor.selectTrack(aTrack)
        val mime = fmt!!.getString(MediaFormat.KEY_MIME)!!
        val sr = fmt!!.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val ch = fmt!!.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val durationUs = if (fmt!!.containsKey(MediaFormat.KEY_DURATION)) fmt!!.getLong(MediaFormat.KEY_DURATION) else 0L

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(fmt, null, null, 0)
        codec.start()

        val inBufs = codec.inputBuffers
        val outInfo = MediaCodec.BufferInfo()
        val outPcm = ArrayList<Short>()

        var sawEOS = false
        while (true) {
            if (!sawEOS) {
                val inIx = codec.dequeueInputBuffer(10_000)
                if (inIx >= 0) {
                    val ib: ByteBuffer = if (Build.VERSION.SDK_INT >= 21) codec.getInputBuffer(inIx)!! else inBufs[inIx]
                    val sampleSize = extractor.readSampleData(ib, 0)
                    val pts = extractor.sampleTime
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inIx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        sawEOS = true
                    } else {
                        codec.queueInputBuffer(inIx, 0, sampleSize, max(0L, pts), 0)
                        extractor.advance()
                    }
                }
            }
            val outIx = codec.dequeueOutputBuffer(outInfo, 10_000)
            when {
                outIx >= 0 -> {
                    val ob: ByteBuffer = if (Build.VERSION.SDK_INT >= 21) codec.getOutputBuffer(outIx)!! else codec.outputBuffers[outIx]
                    val b = ByteArray(outInfo.size)
                    ob.get(b)
                    ob.clear()
                    // Decoder outputs PCM 16-bit
                    val bb = java.nio.ByteBuffer.wrap(b).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    val tmp = ShortArray(bb.remaining())
                    bb.get(tmp)
                    outPcm.addAll(tmp.toList())
                    codec.releaseOutputBuffer(outIx, false)
                    if ((outInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                }
                outIx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { /* ignore */ }
                outIx == MediaCodec.INFO_TRY_AGAIN_LATER -> { /* spin */ }
            }
        }
        codec.stop()
        codec.release()
        extractor.release()
        val pcm = outPcm.toShortArray()
        val frames = pcm.size / ch
        val durMs = if (durationUs > 0) durationUs / 1000 else (frames * 1000L) / sr
        return PCM(sr, ch, pcm, durMs)
    }
    
    /**
     * Load a specific chunk of audio from a file for streaming processing.
     * This allows processing large files in manageable pieces to avoid memory issues.
     */
    fun loadPcm16Chunk(
        ctx: Context,
        uri: Uri,
        startTimeMs: Long,
        endTimeMs: Long
    ): ShortArray {
        val path = uri.toString().lowercase()
        return if (path.endsWith(".wav")) {
            loadWavChunk(ctx, uri, startTimeMs, endTimeMs)
        } else {
            decodeAacMp4Chunk(ctx, uri, startTimeMs, endTimeMs)
        }
    }
    
    private fun loadWavChunk(
        ctx: Context,
        uri: Uri,
        startTimeMs: Long,
        endTimeMs: Long
    ): ShortArray {
        val bytes = ctx.contentResolver.openInputStream(uri)!!.readBytes()
        val wav = Wav.parse(bytes)
        
        val startSample = (startTimeMs * wav.sr * wav.ch / 1000L).toInt()
        val endSample = (endTimeMs * wav.sr * wav.ch / 1000L).toInt()
        val chunkSize = endSample - startSample
        
        return wav.pcm16.sliceArray(startSample until minOf(startSample + chunkSize, wav.pcm16.size))
    }
    
    private fun decodeAacMp4Chunk(
        ctx: Context,
        uri: Uri,
        startTimeMs: Long,
        endTimeMs: Long
    ): ShortArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(ctx, uri, null)
        
        var aTrack = -1
        var fmt: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                aTrack = i
                fmt = f
                break
            }
        }
        require(aTrack >= 0 && fmt != null) { "No audio track" }
        extractor.selectTrack(aTrack)
        
        val mime = fmt!!.getString(MediaFormat.KEY_MIME)!!
        val sr = fmt!!.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val ch = fmt!!.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        
        // Seek to start time
        val startTimeUs = startTimeMs * 1000L
        val endTimeUs = endTimeMs * 1000L
        extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(fmt, null, null, 0)
        codec.start()
        
        val inBufs = codec.inputBuffers
        val outInfo = MediaCodec.BufferInfo()
        val outPcm = ArrayList<Short>()
        
        var sawEOS = false
        var currentTimeUs = startTimeUs
        
        while (currentTimeUs < endTimeUs && !sawEOS) {
            if (!sawEOS) {
                val inIx = codec.dequeueInputBuffer(10_000)
                if (inIx >= 0) {
                    val ib: ByteBuffer = if (Build.VERSION.SDK_INT >= 21) codec.getInputBuffer(inIx)!! else inBufs[inIx]
                    val sampleSize = extractor.readSampleData(ib, 0)
                    val pts = extractor.sampleTime
                    
                    if (sampleSize < 0 || pts >= endTimeUs) {
                        codec.queueInputBuffer(inIx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        sawEOS = true
                    } else {
                        codec.queueInputBuffer(inIx, 0, sampleSize, max(0L, pts), 0)
                        extractor.advance()
                    }
                }
            }
            
            val outIx = codec.dequeueOutputBuffer(outInfo, 10_000)
            when {
                outIx >= 0 -> {
                    val ob: ByteBuffer = if (Build.VERSION.SDK_INT >= 21) codec.getOutputBuffer(outIx)!! else codec.outputBuffers[outIx]
                    val b = ByteArray(outInfo.size)
                    ob.get(b)
                    ob.clear()
                    
                    // Decoder outputs PCM 16-bit
                    val bb = java.nio.ByteBuffer.wrap(b).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    val tmp = ShortArray(bb.remaining())
                    bb.get(tmp)
                    outPcm.addAll(tmp.toList())
                    
                    currentTimeUs = outInfo.presentationTimeUs
                    codec.releaseOutputBuffer(outIx, false)
                    
                    if ((outInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                }
                outIx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { /* ignore */ }
                outIx == MediaCodec.INFO_TRY_AGAIN_LATER -> { /* spin */ }
            }
        }
        
        codec.stop()
        codec.release()
        extractor.release()
        
        return outPcm.toShortArray()
    }
}
