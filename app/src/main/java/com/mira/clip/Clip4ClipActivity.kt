package com.mira.clip

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import com.mira.com.whisper.AndroidWhisperBridge

/**
 * Main activity for mira_clip CLIP4Clip service.
 * 
 * This activity displays the staging interface for video selection and processing.
 */
class Clip4ClipActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "Clip4ClipActivity"
    }
    
    private lateinit var webView: WebView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.i(TAG, "CLIP4Clip app launched - initializing staging interface")
        
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
        
        // Add JavaScript bridge for whisper functionality
        webView.addJavascriptInterface(AndroidWhisperBridge(this), "WhisperBridge")
        
        // Set WebView client
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.i(TAG, "Staging page loaded: $url")
            }
        }
        
        // Load the staging HTML file
        webView.loadUrl("file:///android_asset/web/whisper-file-selection.html")
        
        Log.i(TAG, "Staging interface initialized")
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
