package com.mira.whisper

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
    
    // Permission request launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Log.i(TAG, "Storage permissions granted")
        } else {
            Log.w(TAG, "Some storage permissions denied: $permissions")
        }
    }
    
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
        
        // Request storage permissions first
        requestStoragePermissions()
        
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
        
        // Load the one-page Whisper UI
        webView.loadUrl("file:///android_asset/web/whisper_onepage.html")
        
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
     * Request storage permissions for Android 13+ and older versions
     */
    private fun requestStoragePermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - request READ_MEDIA_VIDEO and READ_MEDIA_AUDIO
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            // Android 12 and below - request READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            Log.i(TAG, "Requesting storage permissions: $permissionsToRequest")
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Log.i(TAG, "All storage permissions already granted")
        }
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
        runOnUiThread {
            webView.evaluateJavascript(
                "if (window.handleFileSelection) { window.handleFileSelection('$jsonResponse'); }",
                null
            )
        }
    }
}