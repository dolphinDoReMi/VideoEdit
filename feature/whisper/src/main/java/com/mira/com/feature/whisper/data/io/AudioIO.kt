package com.mira.com.feature.whisper.data.io

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.util.Log
import java.nio.ByteBuffer
import com.mira.com.core.media.AudioResampler
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
            // Convert to H.264 first for reliability
            val convertedUri = convertToH264IfNeeded(ctx, uri)
            decodeAacMp4(ctx, convertedUri ?: uri)
        }
    }
    
    /**
     * Convert media file to H.264 if needed for reliable processing.
     */
    private fun convertToH264IfNeeded(ctx: Context, uri: Uri): Uri? {
        return try {
            if (MediaConverter.needsConversion(uri)) {
                Log.d("AudioIO", "TECHNICAL: Converting to H.264 for reliable processing: $uri")
                val convertedUri = MediaConverter.convertToH264(ctx, uri)
                if (convertedUri != null) {
                    Log.d("AudioIO", "TECHNICAL: H.264 conversion successful: $convertedUri")
                    convertedUri
                } else {
                    Log.w("AudioIO", "TECHNICAL: H.264 conversion failed, using original file")
                    uri
                }
            } else {
                Log.d("AudioIO", "TECHNICAL: File already in H.264 format: $uri")
                uri
            }
        } catch (e: Exception) {
            Log.e("AudioIO", "TECHNICAL: Error in H.264 conversion: ${e.message}", e)
            uri
        }
    }

    private fun decodeAacMp4(
        ctx: Context,
        uri: Uri,
    ): PCM {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(ctx, uri, null)
        } catch (e: Exception) {
            Log.e("AudioIO", "TECHNICAL: Failed to set data source for $uri: ${e.message}")
            // Try alternative approach for problematic files
            return tryAlternativeExtraction(ctx, uri)
        }
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
        // Prefer conversion for reliability on non-wav inputs
        return if (path.endsWith(".wav")) {
            // Return mono 16k chunk for WAV, consistent with streaming path
            val chunk = loadWavChunk(ctx, uri, startTimeMs, endTimeMs)
            val mono = AudioResampler.downmixToMono(chunk, 1)
            AudioResampler.resampleLinear(mono, 16000, 16000)
        } else {
            // Convert to H.264 if needed before chunk extraction
            val effective = try { convertToH264IfNeeded(ctx, uri) ?: uri } catch (_: Exception) { uri }
            decodeAacMp4Chunk(ctx, effective, startTimeMs, endTimeMs)
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
        try {
            extractor.setDataSource(ctx, uri, null)
        } catch (e: Exception) {
            Log.e("AudioIO", "TECHNICAL: Failed to set data source for chunk extraction $uri: ${e.message}")
            // Try alternative approach for problematic files
            return tryAlternativeChunkExtraction(ctx, uri, startTimeMs, endTimeMs)
        }
        
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
        
        val pcm = outPcm.toShortArray()
        // Normalize to mono 16k for Whisper
        val mono = AudioResampler.downmixToMono(pcm, ch)
        return AudioResampler.resampleLinear(mono, sr, 16000)
    }
    
    /**
     * Alternative extraction method for problematic media files.
     * This method tries different approaches to extract audio from files that fail with standard MediaExtractor.
     */
    private fun tryAlternativeExtraction(
        ctx: Context,
        uri: Uri
    ): PCM {
        Log.d("AudioIO", "TECHNICAL: Attempting alternative extraction for $uri")
        
        // Try with different MediaExtractor configurations
        val extractor = MediaExtractor()
        try {
            // Try with headers parameter
            val headers = HashMap<String, String>()
            headers["User-Agent"] = "Mozilla/5.0"
            extractor.setDataSource(ctx, uri, headers)
            Log.d("AudioIO", "TECHNICAL: Alternative extraction successful with headers")
        } catch (e: Exception) {
            Log.e("AudioIO", "TECHNICAL: Alternative extraction with headers failed: ${e.message}")
            try {
                // Try with file path instead of URI
                val filePath = getFilePathFromUri(ctx, uri)
                if (filePath != null) {
                    extractor.setDataSource(filePath)
                    Log.d("AudioIO", "TECHNICAL: Alternative extraction successful with file path")
                } else {
                    throw Exception("Could not get file path from URI")
                }
            } catch (e2: Exception) {
                Log.e("AudioIO", "TECHNICAL: Alternative extraction with file path failed: ${e2.message}")
                // Return empty PCM as last resort
                return PCM(16000, 1, ShortArray(0), 0L)
            }
        }
        
        // Continue with normal extraction process
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
        
        if (aTrack < 0 || fmt == null) {
            Log.e("AudioIO", "TECHNICAL: No audio track found in alternative extraction")
            return PCM(16000, 1, ShortArray(0), 0L)
        }
        
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
     * Alternative chunk extraction method for problematic media files.
     */
    private fun tryAlternativeChunkExtraction(
        ctx: Context,
        uri: Uri,
        startTimeMs: Long,
        endTimeMs: Long
    ): ShortArray {
        Log.d("AudioIO", "TECHNICAL: Attempting alternative chunk extraction for $uri")
        
        val extractor = MediaExtractor()
        try {
            // Try with headers parameter
            val headers = HashMap<String, String>()
            headers["User-Agent"] = "Mozilla/5.0"
            extractor.setDataSource(ctx, uri, headers)
            Log.d("AudioIO", "TECHNICAL: Alternative chunk extraction successful with headers")
        } catch (e: Exception) {
            Log.e("AudioIO", "TECHNICAL: Alternative chunk extraction with headers failed: ${e.message}")
            try {
                // Try with file path instead of URI
                val filePath = getFilePathFromUri(ctx, uri)
                if (filePath != null) {
                    extractor.setDataSource(filePath)
                    Log.d("AudioIO", "TECHNICAL: Alternative chunk extraction successful with file path")
                } else {
                    throw Exception("Could not get file path from URI")
                }
            } catch (e2: Exception) {
                Log.e("AudioIO", "TECHNICAL: Alternative chunk extraction with file path failed: ${e2.message}")
                // Return empty array as last resort
                return ShortArray(0)
            }
        }
        
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
        
        if (aTrack < 0 || fmt == null) {
            Log.e("AudioIO", "TECHNICAL: No audio track found in alternative chunk extraction")
            return ShortArray(0)
        }
        
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
    
    /**
     * Helper method to get file path from URI
     */
    private fun getFilePathFromUri(ctx: Context, uri: Uri): String? {
        return try {
            val cursor = ctx.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                    if (columnIndex >= 0) {
                        it.getString(columnIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            Log.e("AudioIO", "TECHNICAL: Failed to get file path from URI: ${e.message}")
            null
        }
    }
}
