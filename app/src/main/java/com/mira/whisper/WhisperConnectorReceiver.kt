package com.mira.whisper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.webkit.WebView
import org.json.JSONObject

/**
 * WhisperConnectorReceiver - Broadcast receiver for WhisperConnectorService events.
 * 
 * This receiver handles real-time updates from the connector service and
 * forwards them to the appropriate WebView pages for live updates.
 */
class WhisperConnectorReceiver(
    private val webView: WebView?,
    private val pageName: String
) : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "WhisperConnectorReceiver"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            WhisperConnectorService.ACTION_START_PROCESSING -> {
                handleProcessingStart(intent)
            }
            WhisperConnectorService.ACTION_UPDATE_PROGRESS -> {
                handleProgressUpdate(intent)
            }
            WhisperConnectorService.ACTION_PROCESSING_COMPLETE -> {
                handleProcessingComplete(intent)
            }
            WhisperConnectorService.ACTION_RESOURCE_UPDATE -> {
                handleResourceUpdate(intent)
            }
            WhisperConnectorService.ACTION_PAGE_NAVIGATION -> {
                handlePageNavigation(intent)
            }
        }
    }
    
    /**
     * Handle processing start events
     */
    private fun handleProcessingStart(intent: Intent) {
        val batchId = intent.getStringExtra(WhisperConnectorService.EXTRA_BATCH_ID) ?: return
        val fileCount = intent.getIntExtra(WhisperConnectorService.EXTRA_FILE_COUNT, 0)
        val fileInfoJson = intent.getStringExtra("file_info")
        
        Log.d(TAG, "=== TECHNICAL LOG: Processing started ===")
        Log.d(TAG, "TECHNICAL: Batch ID: $batchId")
        Log.d(TAG, "TECHNICAL: File count: $fileCount")
        Log.d(TAG, "TECHNICAL: Current page: $pageName")
        Log.d(TAG, "TECHNICAL: File info JSON: $fileInfoJson")
        
        // Forward to WebView based on page
        when (pageName) {
            "processing" -> {
                val fileInfoParam = if (fileInfoJson != null) {
                    ", $fileInfoJson"
                } else {
                    ""
                }
                webView?.evaluateJavascript("""
                    if (window.handleProcessingStart) {
                        window.handleProcessingStart('$batchId', $fileCount$fileInfoParam);
                    }
                """, null)
            }
            "results" -> {
                val fileInfoParam = if (fileInfoJson != null) {
                    ", $fileInfoJson"
                } else {
                    ""
                }
                webView?.evaluateJavascript("""
                    if (window.handleProcessingStart) {
                        window.handleProcessingStart('$batchId', $fileCount$fileInfoParam);
                    }
                """, null)
            }
        }
    }
    
    /**
     * Handle progress update events
     */
    private fun handleProgressUpdate(intent: Intent) {
        val batchId = intent.getStringExtra(WhisperConnectorService.EXTRA_BATCH_ID) ?: return
        val progress = intent.getIntExtra(WhisperConnectorService.EXTRA_PROGRESS, 0)
        val fileCount = intent.getIntExtra(WhisperConnectorService.EXTRA_FILE_COUNT, 0)
        val currentFile = intent.getIntExtra(WhisperConnectorService.EXTRA_CURRENT_FILE, 0)
        val hasErrors = intent.getBooleanExtra("has_errors", false)
        val errorMessages = intent.getStringExtra("error_messages")
        val fileStatesJson = intent.getStringExtra("file_states")
        
        Log.d(TAG, "=== TECHNICAL LOG: Progress update ===")
        Log.d(TAG, "TECHNICAL: Batch ID: $batchId")
        Log.d(TAG, "TECHNICAL: Overall progress: $progress%")
        Log.d(TAG, "TECHNICAL: Current file: $currentFile/$fileCount")
        Log.d(TAG, "TECHNICAL: Has errors: $hasErrors")
        Log.d(TAG, "TECHNICAL: Current page: $pageName")
        if (hasErrors && errorMessages != null) {
            Log.e(TAG, "TECHNICAL: Error messages: $errorMessages")
        }
        if (fileStatesJson != null) {
            Log.d(TAG, "TECHNICAL: File states JSON: $fileStatesJson")
        }
        
        // Forward to WebView based on page
        when (pageName) {
            "processing" -> {
                val errorParam = if (hasErrors && errorMessages != null) {
                    ", '$errorMessages'"
                } else {
                    ", null"
                }
                val fileStatesParam = if (fileStatesJson != null) {
                    ", '$fileStatesJson'"
                } else {
                    ", null"
                }
                webView?.evaluateJavascript("""
                    if (window.handleProgressUpdate) {
                        window.handleProgressUpdate('$batchId', $progress, $fileCount, $currentFile$errorParam$fileStatesParam);
                    }
                """, null)
            }
            "results" -> {
                val errorParam = if (hasErrors && errorMessages != null) {
                    ", '$errorMessages'"
                } else {
                    ", null"
                }
                val fileStatesParam = if (fileStatesJson != null) {
                    ", '$fileStatesJson'"
                } else {
                    ", null"
                }
                webView?.evaluateJavascript("""
                    if (window.handleProgressUpdate) {
                        window.handleProgressUpdate('$batchId', $progress, $fileCount, $currentFile$errorParam$fileStatesParam);
                    }
                """, null)
            }
        }
    }
    
    /**
     * Handle processing complete events
     */
    private fun handleProcessingComplete(intent: Intent) {
        val batchId = intent.getStringExtra(WhisperConnectorService.EXTRA_BATCH_ID) ?: return
        
        Log.d(TAG, "Processing complete: $batchId on $pageName")
        
        // Forward to WebView based on page
        when (pageName) {
            "processing" -> {
                webView?.evaluateJavascript("""
                    if (window.handleProcessingComplete) {
                        window.handleProcessingComplete('$batchId');
                    }
                """, null)
            }
            "results" -> {
                webView?.evaluateJavascript("""
                    if (window.handleProcessingComplete) {
                        window.handleProcessingComplete('$batchId');
                    }
                """, null)
            }
        }
    }
    
    /**
     * Handle resource update events
     */
    private fun handleResourceUpdate(intent: Intent) {
        val resourceStatsJson = intent.getStringExtra(WhisperConnectorService.EXTRA_RESOURCE_STATS) ?: return
        
        Log.d(TAG, "Resource update received on $pageName")
        
        // Forward to all pages that support resource monitoring
        when (pageName) {
            "file_selection", "processing", "results" -> {
                webView?.evaluateJavascript("""
                    if (window.updateResourceStats) {
                        window.updateResourceStats('$resourceStatsJson');
                    }
                """, null)
            }
        }
    }
    
    /**
     * Handle page navigation events
     */
    private fun handlePageNavigation(intent: Intent) {
        val targetPage = intent.getStringExtra(WhisperConnectorService.EXTRA_NAVIGATION_TARGET) ?: return
        
        Log.d(TAG, "Navigation request to: $targetPage from $pageName")
        
        // Handle navigation based on current page and target
        when (pageName) {
            "file_selection" -> {
                if (targetPage == "processing") {
                    webView?.evaluateJavascript("""
                        if (window.navigateToProcessing) {
                            window.navigateToProcessing();
                        }
                    """, null)
                }
            }
            "processing" -> {
                if (targetPage == "results") {
                    webView?.evaluateJavascript("""
                        if (window.navigateToResults) {
                            window.navigateToResults();
                        }
                    """, null)
                }
            }
            "results" -> {
                if (targetPage == "file_selection") {
                    webView?.evaluateJavascript("""
                        if (window.navigateToFileSelection) {
                            window.navigateToFileSelection();
                        }
                    """, null)
                }
            }
        }
    }
}
