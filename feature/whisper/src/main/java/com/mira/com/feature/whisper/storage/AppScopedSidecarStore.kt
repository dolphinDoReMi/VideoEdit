package com.mira.com.feature.whisper.storage

import android.content.Context
import android.net.Uri
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * App-scoped sidecar storage implementation.
 * Uses getExternalFilesDir() which requires no runtime permissions and is safe for background workers.
 * 
 * This eliminates EPERM errors by writing to locations the app owns by default.
 */
class AppScopedSidecarStore(private val ctx: Context) : SidecarStore {
    
    private fun baseDir(): File =
        File(ctx.getExternalFilesDir(null), "MiraWhisper/out")
    
    override suspend fun ensureJobDir(jobId: String): SidecarDir {
        val dir = File(baseDir(), "$jobId/sidecars")
        if (!dir.exists()) {
            require(dir.mkdirs()) { "Failed to create job directory: $dir" }
        }
        return SidecarDir(
            uri = Uri.fromFile(dir),
            file = dir,
            description = "app-scoped"
        )
    }
    
    override suspend fun writeJson(jobId: String, name: String, json: String): Uri =
        writeBytes(jobId, name, json.toByteArray(StandardCharsets.UTF_8))
    
    override suspend fun writeBytes(jobId: String, name: String, bytes: ByteArray): Uri {
        val dir = ensureJobDir(jobId).file!!
        val finalFile = File(dir, name)
        val tmpFile = File(dir, "$name.tmp")
        
        // Write to temporary file first
        tmpFile.outputStream().buffered().use { out ->
            out.write(bytes)
            out.flush()
            // Force sync to disk
            (out as java.io.FileOutputStream).fd.sync()
        }
        
        // Atomic commit: prefer rename, fallback to copy+delete
        if (!tmpFile.renameTo(finalFile)) {
            // Fallback: copy then delete temp
            tmpFile.inputStream().use { inp ->
                finalFile.outputStream().use { out -> 
                    inp.copyTo(out)
                }
            }
            tmpFile.delete()
        }
        
        return Uri.fromFile(finalFile)
    }
}
