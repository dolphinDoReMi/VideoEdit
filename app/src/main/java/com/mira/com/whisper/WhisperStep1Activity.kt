package com.mira.com.whisper

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.mira.com.ui.web.StagingJsBridge

/**
 * Activity for Whisper Step-1 UI (Staging - Select & Inspect videos).
 * 
 * Loads the whisper-step1.html WebView with StagingJsBridge for video selection.
 */
class WhisperStep1Activity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "WhisperStep1Activity"
    }
    
    private lateinit var webView: WebView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.i(TAG, "Whisper Step-1 (Staging) activity launched")
        
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
        
        // Add JavaScript bridge for staging functionality
        webView.addJavascriptInterface(StagingJsBridge(this), "StagingBridge")
        
        // Set WebView client
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.i(TAG, "Staging page loaded: $url")
            }
        }
        
        // Load the staging HTML file
        webView.loadUrl("file:///android_asset/web/whisper-step1.html")
        
        Log.i(TAG, "Whisper Step-1 staging interface initialized")
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
