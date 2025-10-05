package com.mira.whisper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.mira.whisper.AndroidWhisperBridge
import android.content.IntentFilter

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
    private lateinit var connectorReceiver: WhisperConnectorReceiver
    
    // File picker launcher (SAF: ACTION_OPEN_DOCUMENT)
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
        
        // Load the whisper file selection HTML file
        webView.loadUrl("file:///android_asset/web/whisper_file_selection.html")
        
        Log.i(TAG, "Whisper File Selection interface initialized")

        // Initialize connector receiver for async navigation and updates
        connectorReceiver = WhisperConnectorReceiver(webView, "file_selection")
        val filter = IntentFilter().apply {
            addAction(WhisperConnectorService.ACTION_START_PROCESSING)
            addAction(WhisperConnectorService.ACTION_UPDATE_PROGRESS)
            addAction(WhisperConnectorService.ACTION_PROCESSING_COMPLETE)
            addAction(WhisperConnectorService.ACTION_RESOURCE_UPDATE)
            addAction(WhisperConnectorService.ACTION_PAGE_NAVIGATION)
        }
        registerReceiver(connectorReceiver, filter)
    }
    
    /**
     * Launch the file picker for video files
     */
    fun launchFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
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

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(connectorReceiver)
        } catch (_: Exception) { }
    }
}
