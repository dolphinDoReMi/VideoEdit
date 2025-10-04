package com.mira.com.whisper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.mira.com.whisper.AndroidWhisperBridge

/**
 * Activity for Whisper File Selection UI.
 * 
 * Loads the whisper_file_selection.html WebView with AndroidWhisper bridge.
 */
class WhisperFileSelectionActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "WhisperFileSelectionActivity"
    }
    
    private lateinit var webView: WebView
    private lateinit var bridge: AndroidWhisperBridge
    
    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val uris = mutableListOf<Uri>()
            
            // Handle single or multiple file selection
            data?.clipData?.let { clipData ->
                // Multiple files selected
                for (i in 0 until clipData.itemCount) {
                    uris.add(clipData.getItemAt(i).uri)
                }
            } ?: data?.data?.let { uri ->
                // Single file selected
                uris.add(uri)
            }
            
            // Pass selected URIs to the bridge
            bridge.handleFileSelection(uris)
        } else {
            Log.d(TAG, "File selection cancelled")
            bridge.handleFileSelection(emptyList())
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.i(TAG, "Whisper File Selection activity launched")
        
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
        
        // Add JavaScript bridge for whisper functionality
        webView.addJavascriptInterface(bridge, "WhisperBridge")
        
        // Set WebView client
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.i(TAG, "Staging page loaded: $url")
            }
        }
        
        // Load the file selection HTML file
        webView.loadUrl("file:///android_asset/web/whisper_file_selection.html")
        
        Log.i(TAG, "Whisper File Selection interface initialized")
    }
    
    /**
     * Launch the file picker for video files
     */
    fun launchFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        
        filePickerLauncher.launch(intent)
    }
    
    /**
     * Notify JavaScript about file selection results
     */
    fun notifyFileSelection(jsonResponse: String) {
        try {
            val script = "if (window.handleFileSelection) { window.handleFileSelection('$jsonResponse'); }"
            webView.evaluateJavascript(script, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error notifying file selection: ${e.message}", e)
        }
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
