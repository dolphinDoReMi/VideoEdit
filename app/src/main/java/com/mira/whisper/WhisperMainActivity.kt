package com.mira.whisper

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.net.Uri
import android.os.PowerManager
import java.io.File

/**
 * Main Whisper Processing Activity
 * 
 * This activity provides a single-page interface that combines all three steps
 * of the whisper processing pipeline into one comprehensive flow:
 * 
 * 1. File Selection & Configuration
 * 2. Real-time Processing & Monitoring  
 * 3. Results Display & Export
 * 
 * The interface dynamically shows/hides sections based on the current processing state,
 * providing a seamless user experience without page navigation.
 */
class WhisperMainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "WhisperMain"
    }
    
    private lateinit var webView: WebView
    private lateinit var bridge: AndroidWhisperBridge
    private var inputFileUri: Uri? = null
    private var outputFolderUri: Uri? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    // File picker launcher (SAF with multiple selection)
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val uris = mutableListOf<Uri>()

            data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            (data.flags ?: 0) and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        )
                    } catch (_: Exception) { }
                    uris.add(uri)
                }
            } ?: data?.data?.let { uri ->
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        (data.flags ?: 0) and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    )
                } catch (_: Exception) { }
                uris.add(uri)
            }

            // Forward selection to bridge → will notify WebView JS
            try {
                bridge.handleFileSelection(uris)
            } catch (e: Exception) {
                Log.e(TAG, "Error forwarding file selection to bridge: ${e.message}", e)
            }
        } else {
            Log.d(TAG, "File selection cancelled")
            try {
                bridge.handleFileSelection(emptyList())
            } catch (_: Exception) { }
        }
    }
    
    // SAF launcher for input file selection
    private val inputFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                Log.d(TAG, "Input file selected: $uri")
                inputFileUri = uri
                // Take persistable permission
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error taking persistable permission for input file", e)
                }
                // Request output folder permission
                requestOutputFolderPermission()
            }
        }
    }
    
    // SAF launcher for output folder selection
    private val outputFolderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                Log.d(TAG, "Output folder selected: $uri")
                outputFolderUri = uri
                // Take persistable permission
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error taking persistable permission for output folder", e)
                }
                // Start Auto-Clipper with SAF permissions
                startAutoClipperWithSAF()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.i(TAG, "Whisper Main Activity launched - initializing interface")
        
        // Acquire wake lock to keep screen on
        acquireWakeLock()
        
        // Ensure app-scoped directory structure exists
        DirectoryManager.ensureDirectoryStructure(this)
        
        // Check and request storage permissions
        checkStoragePermissions()
        
        // Handle direct processing intents
        handleDirectProcessingIntent(intent)
        
        // Initialize WebView
        webView = WebView(this)
        setContentView(webView)
        
        // Configure WebView with crash-safe settings
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            
            // Additional safety settings
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            useWideViewPort = true
            loadWithOverviewMode = true
            
            // Disable potentially problematic features
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            // Note: allowFileAccessFromFileURLs and allowUniversalAccessFromFileURLs are deprecated
            // but still needed for file access functionality
        }
        
        // Force software rendering to avoid Vulkan/GPU crashes
        webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
        
        // Set WebView client with error handling
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page loaded: $url")
            }
            
            @Suppress("DEPRECATION")
            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                Log.e(TAG, "WebView error: $description ($errorCode) for $failingUrl")
                
                // Load fallback page on error
                view?.loadUrl("file:///android_asset/web/whisper_simple.html")
            }
            
            override fun onReceivedHttpError(view: WebView?, request: android.webkit.WebResourceRequest?, errorResponse: android.webkit.WebResourceResponse?) {
                super.onReceivedHttpError(view, request, errorResponse)
                Log.e(TAG, "HTTP error: ${errorResponse?.statusCode} for ${request?.url}")
            }
        }
        
        // Initialize bridge with error handling
        try {
            bridge = AndroidWhisperBridge(this)
            webView.addJavascriptInterface(bridge, "WhisperBridge")
            
            // Provide WebView to bridge for progress/navigation callbacks
            bridge.setWebView(webView)
            Log.i(TAG, "WhisperBridge initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize WhisperBridge: ${e.message}", e)
            // Continue without bridge - the HTML will handle simulation mode
        }
        
        // Try to load the unified interface first (offline-compatible)
        try {
            webView.loadUrl("file:///android_asset/web/whisper_unified.html")
            Log.i(TAG, "Loading unified interface")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load unified interface: ${e.message}", e)
            // Fallback to a minimal error page
            val errorHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Whisper App</title>
                    <style>
                        body { 
                            font-family: Arial, sans-serif; 
                            background-color: #000; 
                            color: #fff; 
                            margin: 20px; 
                        }
                    </style>
                </head>
                <body>
                    <h1>Whisper Transcription</h1>
                    <p>Error loading interface. Please restart the app.</p>
                </body>
                </html>
            """.trimIndent()
            
            webView.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null)
        }
        
        Log.i(TAG, "Whisper Main Activity initialized successfully")
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "New intent received")
        handleDirectProcessingIntent(intent)
    }
    
    /**
     * Handle direct processing intents for automated testing
     * Supports direct_action intents with processing parameters
     */
    private fun handleDirectProcessingIntent(intent: Intent?) {
        if (intent == null) return
        
        // Check for direct_action or regular Intent extras
        val directAction = intent.getStringExtra("direct_action")
        val hasFileUri = intent.getStringExtra("file_uri") != null
        
        if (directAction == "transcribe" || hasFileUri) {
            Log.i(TAG, "=== DIRECT PROCESSING INTENT RECEIVED ===")
            
            val fileUri = intent.getStringExtra("file_uri")
            val modelPath = intent.getStringExtra("model_path")
            val threadCount = intent.getIntExtra("thread_count", 4)
            val language = intent.getStringExtra("language") ?: "auto"
            val maxSeconds = intent.getIntExtra("max_seconds", 0)
            
            Log.i(TAG, "Direct processing parameters:")
            Log.i(TAG, "  File URI: $fileUri")
            Log.i(TAG, "  Model Path: $modelPath")
            Log.i(TAG, "  Thread Count: $threadCount")
            Log.i(TAG, "  Language: $language")
            Log.i(TAG, "  Max Seconds: $maxSeconds")
            
            if (fileUri != null) {
                // Start direct processing via service
                val serviceIntent = Intent(this, com.mira.com.feature.whisper.service.DirectWhisperService::class.java).apply {
                    action = com.mira.com.feature.whisper.service.DirectWhisperService.ACTION_PROCESS_DIRECT
                    putExtra(com.mira.com.feature.whisper.service.DirectWhisperService.EXTRA_URI, fileUri)
                    putExtra(com.mira.com.feature.whisper.service.DirectWhisperService.EXTRA_MODEL, modelPath ?: DirectoryManager.getDefaultWhisperModelPath(this@WhisperMainActivity))
                    putExtra(com.mira.com.feature.whisper.service.DirectWhisperService.EXTRA_THREADS, threadCount)
                    putExtra(com.mira.com.feature.whisper.service.DirectWhisperService.EXTRA_LANG, language)
                    putExtra(com.mira.com.feature.whisper.service.DirectWhisperService.EXTRA_TRANSLATE, false)
                    if (maxSeconds > 0) {
                        putExtra(com.mira.com.feature.whisper.service.DirectWhisperService.EXTRA_MAX_SECONDS, maxSeconds)
                    }
                }
                
                try {
                    startService(serviceIntent)
                    Log.i(TAG, "✅ Direct processing service started")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error starting direct processing service: ${e.message}", e)
                }
            } else {
                Log.e(TAG, "❌ No file_uri provided in direct processing intent")
            }
        }
    }
    
    private fun checkStoragePermissions() {
        Log.d(TAG, "Checking storage permissions...")
        
        // Check if we have the necessary storage permissions
        val hasPermissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - check for READ_MEDIA_VIDEO and READ_MEDIA_AUDIO
            val hasVideoPermission = checkSelfPermission(android.Manifest.permission.READ_MEDIA_VIDEO) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasAudioPermission = checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
            
            Log.d(TAG, "Storage permissions (Android 13+) - Video: $hasVideoPermission, Audio: $hasAudioPermission")
            hasVideoPermission && hasAudioPermission
        } else {
            // Android 12 and below - check for READ_EXTERNAL_STORAGE
            val hasReadPermission = checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
            
            Log.d(TAG, "Storage permissions (Android 12-) - Read: $hasReadPermission")
            hasReadPermission
        }
        
        if (!hasPermissions) {
            Log.d(TAG, "Storage permissions not granted, but not requesting automatically")
            // Don't automatically request permissions on startup - let user initiate file selection
        } else {
            Log.d(TAG, "Storage permissions already granted")
        }
    }
    
    
    private fun requestInputFilePermission() {
        Log.d(TAG, "Requesting SAF permission for input file...")
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        inputFileLauncher.launch(intent)
    }
    
    private fun requestOutputFolderPermission() {
        Log.d(TAG, "Requesting SAF permission for output folder...")
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            // Try to navigate to the Clip folder
            putExtra("android.provider.extra.INITIAL_URI", "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2FClip")
        }
        outputFolderLauncher.launch(intent)
    }
    
    private fun startAutoClipperWithSAF() {
        try {
            Log.d(TAG, "Starting Auto-Clipper with SAF permissions...")
            
            if (inputFileUri == null) {
                Log.e(TAG, "Input file URI is null")
                return
            }
            
            if (outputFolderUri == null) {
                Log.e(TAG, "Output folder URI is null")
                return
            }
            
            Log.d(TAG, "Input URI: $inputFileUri")
            Log.d(TAG, "Output URI: $outputFolderUri")
            
            val autoClipperService = com.mira.clip.autoclip.AutoClipperService(this)
            val workRequest = autoClipperService.processTennisInterview(
                inputVideoUri = inputFileUri!!,
                outputFolderUri = outputFolderUri!!
            )
            
            Log.d(TAG, "Auto-Clipper pipeline started with SAF: ${workRequest.id}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Auto-Clipper with SAF", e)
        }
    }
    
    private fun startAutoClipperDirectly() {
        try {
            Log.d(TAG, "Starting Auto-Clipper directly with app-scoped storage...")
            
            // Use app-scoped storage for input files
            val audioInboxDir = DirectoryManager.getAudioInboxDir(this)
            val videoInboxDir = DirectoryManager.getVideoInboxDir(this)
            
            // Look for input files in app-scoped directories
            val inputFile = listOf(
                File(audioInboxDir, "TennisInterview_converted.mp4"),
                File(videoInboxDir, "TennisInterview_converted.mp4"),
                File(DirectoryManager.getBatchInboxDir(this), "TennisInterview_converted.mp4")
            ).firstOrNull { it.exists() }
            
            if (inputFile == null) {
                Log.e(TAG, "Input file not found in app-scoped directories")
                Log.d(TAG, "Checked directories:")
                Log.d(TAG, "  Audio inbox: ${audioInboxDir.absolutePath}")
                Log.d(TAG, "  Video inbox: ${videoInboxDir.absolutePath}")
                Log.d(TAG, "  Batch inbox: ${DirectoryManager.getBatchInboxDir(this).absolutePath}")
                return
            }
            
            Log.d(TAG, "Found input file at: ${inputFile.absolutePath}")
            
            val inputUri = android.net.Uri.fromFile(inputFile)
            val outputUri = android.net.Uri.fromFile(DirectoryManager.getClipsOutputDir(this))
            
            Log.d(TAG, "Input URI: $inputUri")
            Log.d(TAG, "Output URI: $outputUri")
            
            val autoClipperService = com.mira.clip.autoclip.AutoClipperService(this)
            val workRequest = autoClipperService.processTennisInterview(
                inputVideoUri = inputUri,
                outputFolderUri = outputUri
            )
            
            Log.d(TAG, "Auto-Clipper pipeline started with app-scoped storage: ${workRequest.id}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Auto-Clipper with app-scoped storage", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Whisper Main Activity destroyed")
        
        // Release wake lock
        releaseWakeLock()
    }
    
    /**
     * Launch file picker for media selection
     */
    fun launchFilePicker() {
        Log.d(TAG, "launchFilePicker called")
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("video/*", "image/*"))
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        val fallbackIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("video/*", "image/*"))
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

        val primaryResolver = intent.resolveActivity(packageManager)
        val fallbackResolver = fallbackIntent.resolveActivity(packageManager)
        val finalIntent = when {
            primaryResolver != null -> intent
            fallbackResolver != null -> fallbackIntent
            else -> null
        }

        if (finalIntent == null) {
            Log.e(TAG, "No file picker available on this device")
            return
        }

        filePickerLauncher.launch(finalIntent)
    }
    
    /**
     * Notify JavaScript about file selection results
     */
    fun notifyFileSelection(jsonResponse: String) {
        // This method is called by AndroidWhisperBridge
        // Execute JavaScript to handle the file selection
        runOnUiThread {
            // Escape quotes in JSON response for JavaScript
            val escapedJson = jsonResponse.replace("\"", "\\\"")
            val script = "if (window.handleFileSelection) { window.handleFileSelection(\"$escapedJson\"); }"
            webView.evaluateJavascript(script, null)
            Log.d(TAG, "Executed JavaScript for file selection: $jsonResponse")
        }
    }
    
    /**
     * Acquire wake lock to keep screen on during processing
     */
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "WhisperMainActivity::ScreenWakeLock"
            )
            wakeLock?.acquire(10*60*1000L /*10 minutes*/)
            Log.i(TAG, "Wake lock acquired - screen will stay on")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock: ${e.message}", e)
        }
    }
    
    /**
     * Release wake lock to allow screen to turn off
     */
    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.i(TAG, "Wake lock released - screen can turn off")
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release wake lock: ${e.message}", e)
        }
    }
}