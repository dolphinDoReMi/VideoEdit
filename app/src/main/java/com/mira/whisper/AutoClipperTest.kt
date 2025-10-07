package com.mira.whisper

import android.content.Context
import android.net.Uri
import android.util.Log
import com.mira.clip.autoclip.AutoClipperService
import java.io.File

/**
 * Simple test to trigger Auto-Clipper directly
 */
object AutoClipperTest {
    
    fun startTennisInterviewProcessing(context: Context) {
        try {
            Log.d("AutoClipperTest", "Starting TennisInterview processing...")
            
            // Use the same file access approach as the existing app
            val inputFile = File("/sdcard/Download/TennisInterview_converted.mp4")
            if (!inputFile.exists()) {
                Log.e("AutoClipperTest", "Input file does not exist: ${inputFile.absolutePath}")
                return
            }
            
            val inputUri = Uri.fromFile(inputFile)
            val outputUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADocuments%2FConvertedMedia%2FClip")
            
            Log.d("AutoClipperTest", "Input URI: $inputUri")
            Log.d("AutoClipperTest", "Output URI: $outputUri")
            
            val autoClipperService = AutoClipperService(context)
            val workRequest = autoClipperService.processTennisInterview(
                inputVideoUri = inputUri,
                outputFolderUri = outputUri
            )
            
            Log.d("AutoClipperTest", "Auto-Clipper pipeline started: ${workRequest.id}")
            
        } catch (e: Exception) {
            Log.e("AutoClipperTest", "Error starting Auto-Clipper", e)
        }
    }
}
