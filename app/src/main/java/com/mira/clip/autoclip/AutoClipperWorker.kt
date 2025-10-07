package com.mira.clip.autoclip

import android.content.ContentResolver
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.min

class AutoClipperWorker(appCtx: Context, params: WorkerParameters) : CoroutineWorker(appCtx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        val cr = applicationContext.contentResolver

        val inputUri = Uri.parse(inputData.getString(KEY_INPUT_URI) ?: return@withContext Result.failure(msg("missing INPUT_URI")))
        val outTreeUri = Uri.parse(inputData.getString(KEY_OUT_TREE_URI) ?: return@withContext Result.failure(msg("missing OUT_TREE_URI")))
        val segSec = inputData.getInt(KEY_SEG_SEC, 600).coerceAtLeast(60)

        Log.d("AutoClipperWorker", "Starting auto-clipping: $inputUri -> $outTreeUri (${segSec}s segments)")

        // Determine output target: support both SAF tree (content://) and direct file directory (file://)
        val isFileOutput = outTreeUri.scheme == "file"
        val fileOutDir: File? = if (isFileOutput) {
            val dirPath = outTreeUri.path ?: return@withContext Result.failure(msg("invalid OUT_TREE_URI path"))
            val dir = File(dirPath)
            if (!dir.exists() && !dir.mkdirs()) {
                Log.e("AutoClipperWorker", "Failed to create output dir: $dirPath")
                return@withContext Result.failure(msg("cannot create output dir: $dirPath"))
            }
            if (!dir.isDirectory) {
                Log.e("AutoClipperWorker", "Output path is not a directory: $dirPath")
                return@withContext Result.failure(msg("output path not a directory: $dirPath"))
            }
            dir
        } else null

        val tree: DocumentFile? = if (!isFileOutput) {
            DocumentFile.fromTreeUri(applicationContext, outTreeUri)
                ?: return@withContext Result.failure(msg("invalid OUT_TREE_URI"))
        } else null

        val extractor = MediaExtractor()
        
        // Handle file:// URIs directly
        if (inputUri.scheme == "file") {
            val filePath = inputUri.path ?: return@withContext Result.failure(msg("invalid file path"))
            Log.d("AutoClipperWorker", "Setting data source to file: $filePath")
            try {
                // Try multiple approaches for file access
                val file = java.io.File(filePath)
                if (!file.exists()) {
                    return@withContext Result.failure(msg("File does not exist: $filePath"))
                }
                if (!file.canRead()) {
                    return@withContext Result.failure(msg("Cannot read file: $filePath"))
                }
                
                // Use FileDescriptor approach for better compatibility
                val fileDescriptor = java.io.FileInputStream(file).fd
                extractor.setDataSource(fileDescriptor)
                Log.d("AutoClipperWorker", "Successfully set data source via FileDescriptor")
            } catch (e: Exception) {
                Log.e("AutoClipperWorker", "Failed to set data source: ${e.message}", e)
                return@withContext Result.failure(msg("Failed to set data source: ${e.message}"))
            }
        } else {
            // Handle content:// URIs through ContentResolver (SAF)
            Log.d("AutoClipperWorker", "Setting data source to SAF URI: $inputUri")
            try {
                cr.openFileDescriptor(inputUri, "r").use { pfd ->
                    if (pfd == null) return@withContext Result.failure(msg("cannot open input PFD"))
                    extractor.setDataSource(pfd.fileDescriptor)
                    Log.d("AutoClipperWorker", "Successfully set data source via SAF")
                }
            } catch (e: Exception) {
                Log.e("AutoClipperWorker", "Failed to set data source via SAF: ${e.message}", e)
                return@withContext Result.failure(msg("Failed to set data source via SAF: ${e.message}"))
            }
        }

        val trackCount = extractor.trackCount
        val tracks = buildList {
            for (i in 0 until trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") || mime.startsWith("audio/") || mime.startsWith("text/")) {
                    extractor.selectTrack(i)
                    add(i)
                }
            }
        }
        if (tracks.isEmpty()) {
            extractor.release()
            return@withContext Result.failure(msg("no media tracks"))
        }

        Log.d("AutoClipperWorker", "Found ${tracks.size} media tracks")

        // duration from the container (use the longest track)
        var durUs = 0L
        for (t in tracks) {
            val d = extractor.getTrackFormat(t).getLong(MediaFormat.KEY_DURATION)
            if (d > durUs) durUs = d
        }

        val segUs = segSec * 1_000_000L
        val totalSeg = ((durUs + segUs - 1) / segUs).toInt().coerceAtLeast(1)

        Log.d("AutoClipperWorker", "Duration: ${durUs / 1_000_000}s, creating $totalSeg segments")

        val manifest = JSONArray()
        var segIdx = 0
        var segStartUs = 0L

        // 1MB buffer; adjust if needed
        val buf = ByteBuffer.allocateDirect(1 shl 20)
        val info = MediaCodec.BufferInfo()

        while (segStartUs < durUs) {
            val segEndUs = min(segStartUs + segUs, durUs)
            val clipName = "clip_${segIdx.toString().padStart(3, '0')}.mp4"
            
            Log.d("AutoClipperWorker", "Creating segment $segIdx: $clipName (${segStartUs / 1_000_000}s - ${segEndUs / 1_000_000}s)")
            
            // Prepare muxer based on output mode
            val muxer: MediaMuxer
            val outUriForManifest: Uri
            val outPfd = if (isFileOutput) null else cr.openFileDescriptor(tree!!.createFile("video/mp4", clipName)?.uri
                ?: return@withContext Result.failure(msg("cannot create $clipName")), "rw")
                ?: if (isFileOutput) null else return@withContext Result.failure(msg("cannot open $clipName"))

            Log.d("AutoClipperWorker", "Creating MediaMuxer for: $clipName")
            if (isFileOutput) {
                val outFile = File(fileOutDir, clipName)
                muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                outUriForManifest = Uri.fromFile(outFile)
            } else {
                muxer = MediaMuxer(outPfd!!.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                // We created the file via DocumentFile above; recover its Uri for manifest
                val created = tree!!.findFile(clipName)
                    ?: return@withContext Result.failure(msg("missing created file: $clipName"))
                outUriForManifest = created.uri
            }
            val indexMap = HashMap<Int, Int>(tracks.size)
            for (src in tracks) {
                val trackFormat = extractor.getTrackFormat(src)
                Log.d("AutoClipperWorker", "Adding track $src: ${trackFormat.getString(MediaFormat.KEY_MIME)}")
                indexMap[src] = muxer.addTrack(trackFormat)
            }
            Log.d("AutoClipperWorker", "Starting MediaMuxer for: $clipName")
            muxer.start()

            // Seek each selected track to previous keyframe at segStartUs
            for (src in tracks) extractor.seekTo(segStartUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            var wroteAny = false
            loop@ while (true) {
                var advancedAny = false
                for (src in tracks) {
                    val sampleSize = extractor.readSampleData(buf, 0)
                    if (sampleSize < 0) {
                        // end of stream for this track
                        continue
                    }
                    val ts = extractor.sampleTime
                    if (ts < 0) {
                        extractor.advance()
                        continue
                    }
                    if (ts >= segEndUs) {
                        // reached the segment end for current track
                        // do not advance; next segment will seek again
                        continue
                    }
                    info.offset = 0
                    info.size = sampleSize
                    info.flags = extractor.sampleFlags
                    info.presentationTimeUs = ts
                    
                    try {
                        muxer.writeSampleData(indexMap[src]!!, buf, info)
                        extractor.advance()
                        wroteAny = true
                        advancedAny = true
                    } catch (e: Exception) {
                        Log.e("AutoClipperWorker", "Error writing sample data for track $src: ${e.message}", e)
                        throw e
                    }
                }
                if (!advancedAny) break@loop
            }

            muxer.stop()
            muxer.release()
            if (!isFileOutput) outPfd?.close()

            // Optional: compute sha256 for manifest integrity (small overhead)
            val sha = sha256Stream(cr, outUriForManifest)

            manifest.put(JSONObject().apply {
                put("name", clipName)
                put("uri", outUriForManifest.toString())
                put("segmentIndex", segIdx)
                put("startUs", segStartUs)
                put("endUs", segEndUs)
                put("durationUs", segEndUs - segStartUs)
                put("sha256", sha)
            })

            segIdx += 1
            segStartUs = segEndUs

            setProgress(workDataOf(
                "progress_segments_done" to segIdx,
                "progress_segments_total" to totalSeg
            ))
        }

        extractor.release()

        // Write manifest.json
        val manifestUri: Uri = if (isFileOutput) {
            val mf = File(fileOutDir, "manifest.json")
            FileOutputStream(mf).use { os ->
                val root = JSONObject().apply {
                    put("source", inputUri.toString())
                    put("segmentSeconds", segSec)
                    put("createdEpochMs", System.currentTimeMillis())
                    put("clips", manifest)
                }
                os.write(root.toString(2).encodeToByteArray())
            }
            Uri.fromFile(mf)
        } else {
            val manifestDoc = tree!!.findFile("manifest.json")
                ?: tree.createFile("application/json", "manifest.json")!!
            cr.openOutputStream(manifestDoc.uri, "w")!!.use { os ->
                val root = JSONObject().apply {
                    put("source", inputUri.toString())
                    put("segmentSeconds", segSec)
                    put("createdEpochMs", System.currentTimeMillis())
                    put("clips", manifest)
                }
                os.write(root.toString(2).encodeToByteArray())
            }
            manifestDoc.uri
        }

        Log.d("AutoClipperWorker", "Auto-clipping completed: $segIdx segments created")

        Result.success(workDataOf(
            "clips" to manifest.length(),
            "manifestUri" to manifestUri.toString()
        ))
    }

    private fun msg(m: String) = workDataOf("error" to m)

    private fun sha256Stream(cr: ContentResolver, uri: Uri): String {
        val md = MessageDigest.getInstance("SHA-256")
        cr.openInputStream(uri)?.use { ins ->
            val buf = ByteArray(1 shl 16)
            while (true) {
                val n = ins.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val KEY_INPUT_URI = "INPUT_URI"
        const val KEY_OUT_TREE_URI = "OUT_TREE_URI"
        const val KEY_SEG_SEC = "SEG_SEC"

        fun enqueue(
            context: Context,
            input: Uri,
            outTree: Uri,
            segmentSeconds: Int = 600
        ): WorkRequest {
            val data = workDataOf(
                KEY_INPUT_URI to input.toString(),
                KEY_OUT_TREE_URI to outTree.toString(),
                KEY_SEG_SEC to segmentSeconds
            )
            val req = OneTimeWorkRequestBuilder<AutoClipperWorker>()
                .setInputData(data)
                .addTag("AutoClipper")
                .build()
            WorkManager.getInstance(context).enqueue(req)
            return req
        }
    }
}
