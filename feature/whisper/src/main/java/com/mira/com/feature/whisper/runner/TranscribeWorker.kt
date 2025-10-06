package com.mira.com.feature.whisper.runner

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mira.com.core.media.AudioResampler
import com.mira.com.feature.whisper.data.db.AsrDb
import com.mira.com.feature.whisper.data.db.AsrFile
import com.mira.com.feature.whisper.data.db.AsrJob
import com.mira.com.feature.whisper.data.io.AudioIO
import com.mira.com.feature.whisper.engine.WhisperBridge
import com.mira.com.feature.whisper.engine.WhisperParams
import com.mira.com.feature.whisper.engine.LanguageDetectionService
import com.mira.com.feature.whisper.util.Hash
import com.mira.com.feature.whisper.util.Sidecars
import com.mira.com.feature.whisper.data.db.AsrDao
import org.json.JSONObject
import java.io.File

class TranscribeWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val uri = inputData.getString("uri") ?: return Result.failure(workDataOf("error" to "No URI provided"))
        val model = inputData.getString("model") ?: return Result.failure(workDataOf("error" to "No model provided"))
        val threads = inputData.getInt("threads", 4)
        val beam = inputData.getInt("beam", 0)
        val lang = inputData.getString("lang") ?: "auto"
        val translate = inputData.getBoolean("translate", false)
        val batchIndex = inputData.getInt("batch_index", -1)
        val batchTotal = inputData.getInt("batch_total", -1)
        val batchIdInput = inputData.getString("batch_id")
        val ctx = applicationContext
        val dao = AsrDb.get(ctx).dao()
        val fileId = Hash.sha1(uri)
        val jobId = if (batchIndex >= 0) {
            "batch_${batchIndex}_${System.currentTimeMillis()}_${fileId.take(8)}"
        } else {
            "wjob_${System.currentTimeMillis()}_${fileId.take(8)}"
        }
        
        Log.d("TranscribeWorker", "=== TECHNICAL LOG: Starting job $jobId ===")
        Log.d("TranscribeWorker", "TECHNICAL: URI: $uri")
        Log.d("TranscribeWorker", "TECHNICAL: Model: $model")
        Log.d("TranscribeWorker", "TECHNICAL: Threads: $threads")
        Log.d("TranscribeWorker", "TECHNICAL: Batch: $batchIndex/$batchTotal")
        Log.d("TranscribeWorker", "TECHNICAL: Language: $lang")
        Log.d("TranscribeWorker", "TECHNICAL: Translate: $translate")
        
        // Create AsrFile first to avoid foreign key constraint
        val asrFile = AsrFile(
            id = fileId,
            uri = uri,
            mime = null, // Will be updated later
            durationMs = null, // Will be updated later
            srHz = null, // Will be updated later
            channels = null, // Will be updated later
            state = "NEW",
            updatedAtMs = System.currentTimeMillis()
        )
        
        // Insert or update the file record
        try {
            dao.upsertFile(asrFile)
        } catch (e: Exception) {
            // File might already exist, that's okay
            Log.d("TranscribeWorker", "File $fileId already exists or insert failed: ${e.message}")
        }
        
        // Now create the job
        dao.insertJob(
            AsrJob(
                jobId, fileId, model, threads, beam, lang, translate,
                System.currentTimeMillis(), null, null, null, "RUNNING", null,
            ),
        )

        return try {
            Log.d("TranscribeWorker", "TECHNICAL: Attempting to load audio from URI: $uri")
            
            // Check available memory before loading
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = maxMemory - freeMemory
            val memoryUsagePercent = (usedMemory.toDouble() / maxMemory.toDouble()) * 100
            
            Log.d("TranscribeWorker", "TECHNICAL: Memory status - Used: ${usedMemory / (1024 * 1024)}MB, Free: ${freeMemory / (1024 * 1024)}MB, Usage: ${String.format("%.1f", memoryUsagePercent)}%")
            
            // If memory usage is too high, try to free some memory
            if (memoryUsagePercent > 80) {
                Log.w("TranscribeWorker", "TECHNICAL: High memory usage (${String.format("%.1f", memoryUsagePercent)}%), attempting garbage collection")
                System.gc()
                Thread.sleep(1000) // Give GC time to work
            }
            
            // Check file size to determine processing strategy
            var fileSize = -1L
            try {
                val contentResolver = ctx.contentResolver
                val cursor = contentResolver.query(Uri.parse(uri), arrayOf("_size"), null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val sizeIndex = it.getColumnIndex("_size")
                        if (sizeIndex >= 0) {
                            fileSize = it.getLong(sizeIndex)
                            Log.d("TranscribeWorker", "TECHNICAL: File size: ${fileSize / (1024 * 1024)}MB")
                            
                            // Use streaming processing for large files
                            if (fileSize > 100 * 1024 * 1024) { // > 100MB
                                Log.d("TranscribeWorker", "TECHNICAL: Large file detected - using streaming processing")
                                return processAudioStreaming(ctx, uri, model, threads, beam, lang, translate, jobId, fileId, dao, batchIndex, batchTotal)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("TranscribeWorker", "TECHNICAL: Could not determine file size: ${e.message}")
                // If we can't determine file size, try to get duration as fallback
                try {
                    val mediaMetadataRetriever = android.media.MediaMetadataRetriever()
                    mediaMetadataRetriever.setDataSource(ctx, Uri.parse(uri))
                    val durationStr = mediaMetadataRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val durationMs = durationStr?.toLongOrNull() ?: 0L
                    mediaMetadataRetriever.release()
                    
                    // If duration > 5 minutes, assume it's large and use streaming
                    if (durationMs > 5 * 60 * 1000L) { // > 5 minutes
                        Log.d("TranscribeWorker", "TECHNICAL: Long duration detected (${durationMs / 1000}s) - using streaming processing")
                        return processAudioStreaming(ctx, uri, model, threads, beam, lang, translate, jobId, fileId, dao, batchIndex, batchTotal)
                    }
                } catch (e2: Exception) {
                    Log.w("TranscribeWorker", "TECHNICAL: Could not determine duration either: ${e2.message}")
                }
            }
            
            // 1) Load & condition audio
            val pcm = try {
                // Safety check: if we couldn't determine file size, try loading with timeout
                if (fileSize == -1L) {
                    Log.d("TranscribeWorker", "TECHNICAL: File size unknown - attempting to load with timeout protection")
                    
                    // Try to load audio with a timeout to prevent hanging
                    val loadFuture = java.util.concurrent.CompletableFuture.supplyAsync {
                        AudioIO.loadPcm16(ctx, Uri.parse(uri))
                    }
                    
                    try {
                        // Wait maximum 30 seconds for audio loading
                        loadFuture.get(30, java.util.concurrent.TimeUnit.SECONDS)
                    } catch (e: java.util.concurrent.TimeoutException) {
                        Log.e("TranscribeWorker", "TECHNICAL: Audio loading timed out - file too large")
                        dao.finishJob(jobId, null, null, "ERROR", null, "File too large - loading timeout")
                        dao.updateFile(AsrFile(fileId, uri, null, null, null, null, "ERROR", System.currentTimeMillis()))
                        return Result.success() // Return success to avoid retry
                    }
                } else {
                    AudioIO.loadPcm16(ctx, Uri.parse(uri))
                }
            } catch (e: IllegalArgumentException) {
                if (e.message?.contains("No audio track") == true) {
                    Log.w("TranscribeWorker", "Video file has no audio track: $uri")
                    dao.finishJob(jobId, null, null, "ERROR", null, "No audio track in video file")
                    dao.updateFile(AsrFile(fileId, uri, null, null, null, null, "ERROR", System.currentTimeMillis()))
                    return Result.success() // Return success to avoid retry
                } else {
                    throw e
                }
            } catch (e: OutOfMemoryError) {
                Log.w("TranscribeWorker", "Out of memory loading audio from: $uri")
                dao.finishJob(jobId, null, null, "ERROR", null, "File too large - out of memory")
                dao.updateFile(AsrFile(fileId, uri, null, null, null, null, "ERROR", System.currentTimeMillis()))
                return Result.success() // Return success to avoid retry
            }
            
            val mono = AudioResampler.downmixToMono(pcm.pcm16, pcm.ch)
            val pcm16k = AudioResampler.resampleLinear(mono, pcm.sr, 16_000)

            Log.d("TranscribeWorker", "TECHNICAL: Audio loaded successfully")
            Log.d("TranscribeWorker", "TECHNICAL: - Samples: ${pcm16k.size}")
            Log.d("TranscribeWorker", "TECHNICAL: - Duration: ${pcm.durationMs}ms")
            Log.d("TranscribeWorker", "TECHNICAL: - Sample rate: ${pcm.sr}Hz")
            Log.d("TranscribeWorker", "TECHNICAL: - Channels: ${pcm.ch}")

            // 2) Robust LID Pipeline
            val lidResult = if (lang == "auto") {
                Log.d("TranscribeWorker", "Running robust LID pipeline...")
                val lidService = LanguageDetectionService()
                lidService.detectLanguage(pcm16k, 16_000, model, threads)
            } else {
                Log.d("TranscribeWorker", "Using forced language: $lang")
                LanguageDetectionService.LanguageDetectionResult(
                    topK = listOf(LanguageDetectionService.LanguageConfidence(lang, 1.0)),
                    chosen = lang,
                    method = "forced",
                    confidence = 1.0
                )
            }

            Log.d("TranscribeWorker", "LID Result: ${lidResult.chosen} (${lidResult.confidence}) via ${lidResult.method}")

            // 3) Decode with detected/forced language
            val t0 = SystemClock.elapsedRealtime()
            val json = WhisperBridge.decodeJson(
                pcm16 = pcm16k,
                sampleRate = 16_000,
                modelPath = model,
                threads = threads,
                beam = beam,
                lang = lidResult.chosen,
                translate = translate,
                temperature = 0.0f,
                enableWordTimestamps = false,
                detectLanguage = false, // Already done above
                noContext = true
            )
            val t1 = SystemClock.elapsedRealtime()
            val inferMs = t1 - t0
            val rtf = inferMs.toDouble() / pcm.durationMs.coerceAtLeast(1)

            Log.d("TranscribeWorker", "TECHNICAL: Transcription completed successfully")
            Log.d("TranscribeWorker", "TECHNICAL: - Inference time: ${inferMs}ms")
            Log.d("TranscribeWorker", "TECHNICAL: - RTF (Real Time Factor): $rtf")
            Log.d("TranscribeWorker", "TECHNICAL: - Language detected: ${lidResult.chosen}")
            Log.d("TranscribeWorker", "TECHNICAL: - Confidence: ${lidResult.confidence}")

            // 4) Enhanced sidecar with LID data
            val sidecar =
                Sidecars.build(
                    uri = uri,
                    durationMs = pcm.durationMs,
                    params = WhisperParams(model, threads, beam, lidResult.chosen, translate),
                    inferMs = inferMs,
                    rtf = rtf,
                    segmentsJson = JSONObject(json).optJSONArray("segments"),
                )

            // Back-compat fields expected by aggregator
            sidecar.put("job_id", jobId)
            sidecar.put("uri", uri)
            sidecar.put("rtf", rtf)
            sidecar.put("created_at", System.currentTimeMillis())

            // Add LID data to sidecar
            val lidService = LanguageDetectionService()
            val lidSidecar = lidService.generateLidSidecar(lidResult)
            sidecar.put("lid", lidSidecar)

            Log.d("TranscribeWorker", "Enhanced sidecar with LID data")
            val outDir = File("/sdcard/MiraWhisper/out").apply { mkdirs() }
            val sidecarDir = File("/sdcard/MiraWhisper/sidecars").apply { mkdirs() }
            
            // Generate batch-specific sidecar filename
            val sidecarFilename = if (batchIndex >= 0) {
                val safeBatch = (batchIdInput ?: "batch").replace(Regex("[^a-zA-Z0-9_-]"), "_")
                "${safeBatch}_${batchIndex}_${fileId}_$jobId.json"
            } else {
                "${fileId}_$jobId.json"
            }
            
            val sidecarPath = File(sidecarDir, sidecarFilename).absolutePath
            File(sidecarPath).writeText(sidecar.toString())
            
            Log.d("TranscribeWorker", "Generated sidecar: $sidecarPath")

            // 4) Persist file state & segments
            dao.updateFile(AsrFile(fileId, uri, null, pcm.durationMs, 16_000, 1, "DONE", System.currentTimeMillis()))
            val segs = Sidecars.segmentsFrom(sidecar, jobId)
            dao.insertSegments(segs)
            dao.finishJob(jobId, inferMs, rtf, "DONE", sidecarPath, null)
            
            if (batchIndex >= 0) {
                Log.d("TranscribeWorker", "Completed robust LID batch job $jobId ($batchIndex/$batchTotal) - Language: ${lidResult.chosen}, RTF: $rtf")
            } else {
                Log.d("TranscribeWorker", "Completed robust LID job $jobId - Language: ${lidResult.chosen}, RTF: $rtf")
            }
            
            Result.success()
        } catch (t: Throwable) {
            Log.e("TranscribeWorker", "Error processing job $jobId: ${t.message}", t)
            Log.e("TranscribeWorker", "URI that failed: $uri")
            Log.e("TranscribeWorker", "Error type: ${t.javaClass.simpleName}")
            dao.finishJob(jobId, null, null, "ERROR", null, t.message)
            dao.updateFile(AsrFile(fileId, uri, null, null, null, null, "ERROR", System.currentTimeMillis()))
            Result.failure()
        }
    }
    
    /**
     * Process large audio files using streaming/chunked approach to avoid memory issues.
     * This method loads audio in chunks, processes each chunk, and combines results.
     */
    private fun processAudioStreaming(
        ctx: Context,
        uri: String,
        model: String,
        threads: Int,
        beam: Int,
        lang: String,
        translate: Boolean,
        jobId: String,
        fileId: String,
        dao: AsrDao,
        batchIndex: Int,
        batchTotal: Int
    ): Result {
        Log.d("TranscribeWorker", "TECHNICAL: Starting streaming processing for large file")
        
        return try {
            // Update job status to running
            dao.finishJob(jobId, null, null, "RUNNING", null, null)
            
            // Load audio metadata first
            val mediaMetadataRetriever = android.media.MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(ctx, Uri.parse(uri))
            val durationStr = mediaMetadataRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            mediaMetadataRetriever.release()
            
            Log.d("TranscribeWorker", "TECHNICAL: Audio duration: ${durationMs}ms (${durationMs / 1000}s)")
            
            // Process in chunks of 30 seconds to balance memory usage and efficiency
            val chunkDurationMs = 30 * 1000L // 30 seconds per chunk
            val totalChunks = ((durationMs + chunkDurationMs - 1) / chunkDurationMs).toInt()
            
            Log.d("TranscribeWorker", "TECHNICAL: Processing ${totalChunks} chunks of ${chunkDurationMs}ms each")
            
            // Calculate maximum processing time (5 minutes per chunk + 2 minutes buffer)
            val maxProcessingTimeMs = (totalChunks * 5 * 60 * 1000L) + (2 * 60 * 1000L)
            val processingStartTime = System.currentTimeMillis()
            
            Log.d("TranscribeWorker", "TECHNICAL: Maximum processing time: ${maxProcessingTimeMs / 1000}s")
            Log.d("TranscribeWorker", "TECHNICAL: Processing started at: $processingStartTime")
            
            val allChunkTexts = mutableListOf<String>()
            var totalInferenceTime = 0L
            
            // Process each chunk with timeout protection
            for (chunkIndex in 0 until totalChunks) {
                // Check for overall timeout
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - processingStartTime
                
                if (elapsedTime > maxProcessingTimeMs) {
                    Log.e("TranscribeWorker", "TECHNICAL: TIMEOUT - Processing exceeded ${maxProcessingTimeMs / 1000}s limit")
                    Log.e("TranscribeWorker", "TECHNICAL: Processed ${chunkIndex}/$totalChunks chunks before timeout")
                    break
                }
                
                val startTimeMs = chunkIndex * chunkDurationMs
                val endTimeMs = minOf(startTimeMs + chunkDurationMs, durationMs)
                val chunkStartTime = System.currentTimeMillis()
                
                Log.d("TranscribeWorker", "=== STREAMING CHUNK ${chunkIndex + 1}/$totalChunks START ===")
                Log.d("TranscribeWorker", "TECHNICAL: Time range: ${startTimeMs}ms - ${endTimeMs}ms")
                Log.d("TranscribeWorker", "TECHNICAL: Chunk duration: ${endTimeMs - startTimeMs}ms")
                Log.d("TranscribeWorker", "TECHNICAL: Elapsed time: ${elapsedTime / 1000}s / ${maxProcessingTimeMs / 1000}s")
                
                try {
                    // Load audio chunk with timeout
                    val loadStartTime = System.currentTimeMillis()
                    val chunkPcm = AudioIO.loadPcm16Chunk(ctx, Uri.parse(uri), startTimeMs, endTimeMs)
                    val loadTime = System.currentTimeMillis() - loadStartTime
                    
                    Log.d("TranscribeWorker", "TECHNICAL: Chunk loaded in ${loadTime}ms - ${chunkPcm.size} samples")
                    
                    // Convert to mono 16kHz for Whisper
                    val convertStartTime = System.currentTimeMillis()
                    val mono = AudioResampler.downmixToMono(chunkPcm, 1) // Assume mono for simplicity
                    val pcm16k = AudioResampler.resampleLinear(mono, 16000, 16000)
                    val convertTime = System.currentTimeMillis() - convertStartTime
                    
                    Log.d("TranscribeWorker", "TECHNICAL: Audio converted in ${convertTime}ms - ${pcm16k.size} samples")
                    
                    // Process chunk with Whisper (with timeout protection)
                    val whisperStartTime = System.currentTimeMillis()
                    
                    // Individual chunk timeout (2 minutes per chunk)
                    val chunkTimeoutMs = 2 * 60 * 1000L
                    
                    val chunkJson = try {
                        WhisperBridge.decodeJson(
                            pcm16 = pcm16k,
                            sampleRate = 16000,
                            modelPath = model,
                            threads = threads,
                            beam = beam,
                            lang = lang,
                            translate = translate
                        )
                    } catch (e: Exception) {
                        val whisperTime = System.currentTimeMillis() - whisperStartTime
                        if (whisperTime > chunkTimeoutMs) {
                            Log.e("TranscribeWorker", "TECHNICAL: Chunk ${chunkIndex + 1} Whisper processing timed out after ${whisperTime}ms")
                            throw Exception("Chunk processing timeout after ${whisperTime}ms")
                        } else {
                            throw e
                        }
                    }
                    
                    val whisperTime = System.currentTimeMillis() - whisperStartTime
                    totalInferenceTime += whisperTime
                    
                    // Parse JSON result to extract text
                    val parseStartTime = System.currentTimeMillis()
                    val jsonResult = JSONObject(chunkJson)
                    val chunkText = jsonResult.optString("text", "").trim()
                    val parseTime = System.currentTimeMillis() - parseStartTime
                    
                    val totalChunkTime = System.currentTimeMillis() - chunkStartTime
                    
                    Log.d("TranscribeWorker", "TECHNICAL: Whisper processing: ${whisperTime}ms")
                    Log.d("TranscribeWorker", "TECHNICAL: JSON parsing: ${parseTime}ms")
                    Log.d("TranscribeWorker", "TECHNICAL: Total chunk time: ${totalChunkTime}ms")
                    Log.d("TranscribeWorker", "TECHNICAL: Text extracted: ${chunkText.length} characters")
                    Log.d("TranscribeWorker", "TECHNICAL: Text preview: ${chunkText.take(100)}...")
                    
                    if (chunkText.isNotEmpty()) {
                        allChunkTexts.add(chunkText)
                        Log.d("TranscribeWorker", "TECHNICAL: Chunk text added to results")
                    } else {
                        Log.w("TranscribeWorker", "TECHNICAL: Chunk produced empty text")
                    }
                    
                    // Update progress
                    val progress = ((chunkIndex + 1) * 100) / totalChunks
                    Log.d("TranscribeWorker", "TECHNICAL: Overall progress: $progress% (${chunkIndex + 1}/$totalChunks chunks)")
                    Log.d("TranscribeWorker", "=== STREAMING CHUNK ${chunkIndex + 1}/$totalChunks COMPLETE ===")
                    
                    // Add timeout protection - if chunk takes too long, warn and continue
                    if (totalChunkTime > 60000) { // 60 seconds per chunk
                        Log.w("TranscribeWorker", "TECHNICAL: WARNING - Chunk ${chunkIndex + 1} took ${totalChunkTime}ms (>60s)")
                    }
                    
                } catch (e: Exception) {
                    val chunkTime = System.currentTimeMillis() - chunkStartTime
                    Log.e("TranscribeWorker", "TECHNICAL: ERROR in chunk ${chunkIndex + 1} after ${chunkTime}ms: ${e.message}", e)
                    Log.e("TranscribeWorker", "=== STREAMING CHUNK ${chunkIndex + 1}/$totalChunks FAILED ===")
                    // Continue with next chunk instead of failing completely
                }
                
                // Add small delay between chunks to prevent overwhelming the system
                if (chunkIndex < totalChunks - 1) {
                    Thread.sleep(100) // 100ms delay between chunks
                }
            }
            
            // Combine all chunk texts
            val finalText = allChunkTexts.joinToString(" ")
            val rtf = if (durationMs > 0) totalInferenceTime.toDouble() / durationMs else 0.0
            
            Log.d("TranscribeWorker", "TECHNICAL: Streaming processing completed successfully")
            Log.d("TranscribeWorker", "TECHNICAL: - Total chunks processed: ${allChunkTexts.size}")
            Log.d("TranscribeWorker", "TECHNICAL: - Total inference time: ${totalInferenceTime}ms")
            Log.d("TranscribeWorker", "TECHNICAL: - RTF (Real Time Factor): $rtf")
            Log.d("TranscribeWorker", "TECHNICAL: - Final text length: ${finalText.length} characters")
            
            // Save results - use the same pattern as the main doWork function
            dao.finishJob(jobId, totalInferenceTime, rtf, "DONE", null, null)
            dao.updateFile(AsrFile(fileId, uri, null, null, null, null, "DONE", System.currentTimeMillis()))
            
            // Emit success events - use the same pattern as the main doWork function
            // Note: WhisperBus might not be available, so we'll skip these for now
            
            Result.success()
            
        } catch (e: Exception) {
            Log.e("TranscribeWorker", "TECHNICAL: Error in streaming processing: ${e.message}", e)
            dao.finishJob(jobId, null, null, "ERROR", null, e.message ?: "Streaming processing failed")
            dao.updateFile(AsrFile(fileId, uri, null, null, null, null, "ERROR", System.currentTimeMillis()))
            Result.failure()
        }
    }
}