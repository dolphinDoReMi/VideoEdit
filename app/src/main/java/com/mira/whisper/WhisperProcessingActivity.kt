package com.mira.whisper

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import android.util.Log

/**
 * Activity for Whisper Processing UI.
 * 
 * Loads the whisper-processing.html WebView with AndroidWhisper bridge.
 */
class WhisperProcessingActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "WhisperProcessingActivity"
    }
    
    private lateinit var webView: WebView
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "Creating WhisperProcessingActivity")
        
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "Page finished loading: $url")
                }
                
                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Log.e(TAG, "WebView error: $description ($errorCode) for $failingUrl")
                }
            }
            
            // Add the WhisperBridge
            addJavascriptInterface(AndroidWhisperBridge(this@WhisperProcessingActivity), "WhisperBridge")
            
            // Load the consolidated processing page
            loadUrl("file:///android_asset/web/whisper_processing.html")
        }
        
        setContentView(webView)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Destroying WhisperProcessingActivity")
    }
}
