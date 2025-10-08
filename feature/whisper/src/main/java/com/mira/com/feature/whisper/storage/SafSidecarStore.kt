package com.mira.com.feature.whisper.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.nio.charset.StandardCharsets

/**
 * SAF (Storage Access Framework) sidecar storage implementation.
 * Uses DocumentFile API for writing to user-picked directories.
 * 
 * Note: This requires a persisted URI permission grant from the user.
 * Use only when explicitly writing to user-selected locations.
 */
class SafSidecarStore(
    val ctx: Context,
    private val treeUri: Uri
) : SidecarStore {
    
    val context: Context get() = ctx
    
    private val tree: DocumentFile by lazy {
        DocumentFile.fromTreeUri(ctx, treeUri)
            ?: error("Invalid SAF tree URI: $treeUri")
    }
    
    override suspend fun ensureJobDir(jobId: String): SidecarDir {
        require(tree.canWrite()) { "SAF tree not writable: $treeUri" }
        
        // Find or create job directory
        var jobDir = tree.findFile(jobId)
        if (jobDir == null) {
            jobDir = tree.createDirectory(jobId)
                ?: error("Cannot create job directory: $jobId")
        }
        
        // Find or create sidecars subdirectory
        val sidecarsDir = jobDir.findFile("sidecars") 
            ?: jobDir.createDirectory("sidecars")
            ?: error("Cannot create sidecars directory")
        
        return SidecarDir(
            uri = sidecarsDir.uri,
            file = null, // SAF doesn't provide File objects
            description = "saf-tree"
        )
    }
    
    override suspend fun writeJson(jobId: String, name: String, json: String): Uri =
        writeBytes(jobId, name, json.toByteArray(StandardCharsets.UTF_8))
    
    override suspend fun writeBytes(jobId: String, name: String, bytes: ByteArray): Uri {
        val dir = ensureJobDir(jobId)
        val parent = DocumentFile.fromTreeUri(ctx, dir.uri)
            ?: error("Cannot access sidecars directory")
        
        // Create temporary file
        val tmpFile = parent.createFile("application/octet-stream", "$name.tmp")
            ?: error("Cannot create temporary file: $name.tmp")
        
        // Write to temporary file
        ctx.contentResolver.openOutputStream(tmpFile.uri, "wt")!!.use { out ->
            out.write(bytes)
        }
        
        // Commit: create final file and copy from temp
        // Note: SAF doesn't support atomic rename, so we copy then delete temp
        parent.findFile(name)?.delete() // Remove existing file if any
        
        val finalFile = parent.createFile("application/json", name)
            ?: error("Cannot create final file: $name")
        
        // Copy temp to final
        ctx.contentResolver.openInputStream(tmpFile.uri)!!.use { inp ->
            ctx.contentResolver.openOutputStream(finalFile.uri, "wt")!!.use { out ->
                inp.copyTo(out)
            }
        }
        
        // Clean up temporary file
        tmpFile.delete()
        
        return finalFile.uri
    }
}
