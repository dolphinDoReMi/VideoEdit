package com.mira.com.whisper

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
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
        
        // Add JavaScript bridge for whisper functionality
        webView.addJavascriptInterface(AndroidWhisperBridge(this), "WhisperBridge")
        
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
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
