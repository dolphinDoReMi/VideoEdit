package com.mira.whisper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.mira.whisper.AndroidWhisperBridge

/**
 * Main activity for Whisper video transcription service.
 * 
 * This activity displays the Whisper interface for video selection and processing.
 */
class WhisperMainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "WhisperMainActivity"
    }
    
    private lateinit var webView: WebView
    private lateinit var bridge: AndroidWhisperBridge
    
    // File picker launcher (SAF: ACTION_OPEN_DOCUMENT)
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val uris = mutableListOf<Uri>()

            // Handle single or multiple file selection
            data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    // Persist read permission for future access
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

            bridge.handleFileSelection(uris)
        } else {
            Log.d(TAG, "File selection cancelled")
            bridge.handleFileSelection(emptyList())
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.i(TAG, "Whisper app launched - initializing interface")
        
        // Initialize WebView
        webView = WebView(this)
        setContentView(webView)
        
        // Configure WebView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
        }
        
        // Initialize bridge
        bridge = AndroidWhisperBridge(this)
        
        // Set the activity reference in bridge for file picker
        bridge.setActivity(this)
        
        // Add JavaScript bridge for whisper functionality
        webView.addJavascriptInterface(bridge, "WhisperBridge")
        
        // Set WebView client
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.i(TAG, "Whisper page loaded: $url")
            }
        }
        
        // Load the unified whisper page (all-in-one interface)
        webView.loadUrl("file:///android_asset/web/whisper_unified.html")
        
        Log.i(TAG, "Whisper interface initialized")
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
    
    
    
    /**
     * Launch the file picker for video and photo files with multiple selection
     */
    fun launchFilePicker() {
        // Try multiple approaches for better multiple selection support
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("video/*", "image/*"))
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        
        // Fallback to ACTION_OPEN_DOCUMENT if GET_CONTENT doesn't work
        val fallbackIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("video/*", "image/*"))
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        
        // Check which intent can be resolved
        val primaryResolver = intent.resolveActivity(packageManager)
        val fallbackResolver = fallbackIntent.resolveActivity(packageManager)
        
        val finalIntent = when {
            primaryResolver != null -> intent
            fallbackResolver != null -> fallbackIntent
            else -> {
                Log.e(TAG, "No file picker available on this device")
                return
            }
        }
        
        Log.d(TAG, "Launching file picker with multiple selection support")
        filePickerLauncher.launch(finalIntent)
    }
    
    /**
     * Notify JavaScript about file selection results
     */
    fun notifyFileSelection(jsonResponse: String) {
        runOnUiThread {
            webView.evaluateJavascript(
                "if (window.handleFileSelection) { window.handleFileSelection('$jsonResponse'); }",
                null
            )
        }
    }
}