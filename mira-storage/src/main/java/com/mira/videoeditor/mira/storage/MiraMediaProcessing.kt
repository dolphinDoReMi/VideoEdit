package com.mira.videoeditor.mira.storage

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * Media processing interface for scoped storage compliant audio/video operations.
 * 
 * This interface provides capability-based media processing that works with
 * file descriptors and scoped storage APIs.
 */
interface MiraMediaProcessing {
    
    /**
     * Decodes WAV audio from a file descriptor to PCM data.
     * 
     * @param audioFd File descriptor to the WAV file
     * @param targetSampleRate Target sample rate for the PCM data
     * @param mono Whether to convert to mono
     * @return PCM audio data as ByteArray
     */
    suspend fun decodeWavFromFd(audioFd: java.io.FileDescriptor, targetSampleRate: Int, mono: Boolean): ByteArray
    
    /**
     * Extracts audio from a video file descriptor.
     * 
     * @param videoFd File descriptor to the video file
     * @param targetSampleRate Target sample rate for the audio
     * @param mono Whether to convert to mono
     * @return PCM audio data as ByteArray
     */
    suspend fun extractAudioFromVideo(videoFd: java.io.FileDescriptor, targetSampleRate: Int, mono: Boolean): ByteArray
    
    /**
     * Gets media file information from a file descriptor.
     * 
     * @param mediaFd File descriptor to the media file
     * @return MediaInfo with duration, sample rate, channels, etc.
     */
    suspend fun getMediaInfo(mediaFd: java.io.FileDescriptor): MediaInfo
}

/**
 * Media information data class.
 */
data class MediaInfo(
    val duration: Float,
    val sampleRate: Int,
    val channels: Int,
    val format: String,
    val size: Long
)

/**
 * Android implementation of MediaProcessing using MediaCodec and scoped storage.
 */
class AndroidMiraMediaProcessing(
    private val context: android.content.Context
) : MiraMediaProcessing {
    
    companion object {
        private const val TAG = "AndroidMiraMediaProcessing"
    }
    
    override suspend fun decodeWavFromFd(audioFd: java.io.FileDescriptor, targetSampleRate: Int, mono: Boolean): ByteArray {
        // TODO: Implement proper WAV decoding from file descriptor
        // This would use MediaCodec or native audio libraries
        // to decode WAV to PCM with proper sample rate conversion
        
        // For now, return mock PCM data
        return ByteArray(16000 * 2) // 1 second of 16kHz stereo PCM
    }
    
    override suspend fun extractAudioFromVideo(videoFd: java.io.FileDescriptor, targetSampleRate: Int, mono: Boolean): ByteArray {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Extracting audio from video file descriptor")
            
            try {
                val extractor = MediaExtractor()
                extractor.setDataSource(videoFd)
                
                // Find audio track
                var audioTrackIndex = -1
                var audioFormat: MediaFormat? = null
                
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME)
                    if (mime?.startsWith("audio/") == true) {
                        audioTrackIndex = i
                        audioFormat = format
                        break
                    }
                }
                
                if (audioTrackIndex == -1) {
                    Log.e(TAG, "No audio track found")
                    return@withContext ByteArray(0)
                }
                
                extractor.selectTrack(audioTrackIndex)
                
                // Create decoder
                val decoder = MediaCodec.createDecoderByType(audioFormat!!.getString(MediaFormat.KEY_MIME)!!)
                decoder.configure(audioFormat, null, null, 0)
                decoder.start()
                
                val audioData = mutableListOf<ByteArray>()
                var isEOS = false
                val bufferInfo = android.media.MediaCodec.BufferInfo()
                
                while (!isEOS) {
                    val inputBufferIndex = decoder.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputBufferIndex)
                        val sampleSize = extractor.readSampleData(inputBuffer!!, 0)
                        
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEOS = true
                        } else {
                            decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                    
                    val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
                    if (outputBufferIndex >= 0) {
                        val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            val audioChunk = ByteArray(bufferInfo.size)
                            outputBuffer.get(audioChunk)
                            audioData.add(audioChunk)
                        }
                        decoder.releaseOutputBuffer(outputBufferIndex, false)
                    }
                }
                
                decoder.stop()
                decoder.release()
                extractor.release()
                
                // Combine all audio chunks
                val totalSize = audioData.sumOf { it.size }
                val combinedAudio = ByteArray(totalSize)
                var offset = 0
                for (chunk in audioData) {
                    System.arraycopy(chunk, 0, combinedAudio, offset, chunk.size)
                    offset += chunk.size
                }
                
                Log.d(TAG, "Audio extraction completed: ${combinedAudio.size} bytes")
                combinedAudio
                
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting audio from video", e)
                ByteArray(0)
            }
        }
    }
    
    override suspend fun getMediaInfo(mediaFd: java.io.FileDescriptor): MediaInfo {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Getting media info from file descriptor")
            
            try {
                val extractor = MediaExtractor()
                extractor.setDataSource(mediaFd)
                
                var duration = 0L
                var sampleRate = 0
                var channels = 0
                var format = "unknown"
                
                for (i in 0 until extractor.trackCount) {
                    val trackFormat = extractor.getTrackFormat(i)
                    val mime = trackFormat.getString(MediaFormat.KEY_MIME)
                    
                    if (mime?.startsWith("audio/") == true) {
                        sampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        format = mime
                    }
                    
                    if (trackFormat.containsKey(MediaFormat.KEY_DURATION)) {
                        duration = maxOf(duration, trackFormat.getLong(MediaFormat.KEY_DURATION))
                    }
                }
                
                extractor.release()
                
                val mediaInfo = MediaInfo(
                    duration = duration / 1000000f, // Convert microseconds to seconds
                    sampleRate = sampleRate,
                    channels = channels,
                    format = format,
                    size = 0L // File size not available from MediaExtractor
                )
                
                Log.d(TAG, "Media info: $mediaInfo")
                mediaInfo
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting media info", e)
                MediaInfo(0f, 0, 0, "error", 0L)
            }
        }
    }
}
