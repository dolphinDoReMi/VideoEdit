package com.mira.com.feature.whisper.storage

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Abstraction for sidecar storage operations.
 * Provides a unified interface for writing sidecar files regardless of storage backend.
 */
interface SidecarStore {
    /**
     * Ensure the job directory exists and return its metadata.
     * @param jobId Unique job identifier
     * @return SidecarDir containing URI, File reference (if applicable), and description
     */
    suspend fun ensureJobDir(jobId: String): SidecarDir
    
    /**
     * Write JSON content to a sidecar file.
     * @param jobId Job identifier
     * @param name Filename (without extension)
     * @param json JSON content as string
     * @return Uri of the written file
     */
    suspend fun writeJson(jobId: String, name: String, json: String): Uri
    
    /**
     * Write binary content to a sidecar file.
     * @param jobId Job identifier
     * @param name Filename (without extension)
     * @param bytes Binary content
     * @return Uri of the written file
     */
    suspend fun writeBytes(jobId: String, name: String, bytes: ByteArray): Uri
}

/**
 * Metadata about a sidecar directory.
 */
data class SidecarDir(
    val uri: Uri,
    val file: File?, // null for SAF-based storage
    val description: String
)
