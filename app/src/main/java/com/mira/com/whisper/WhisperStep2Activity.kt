package com.mira.com.whisper

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import android.util.Log

/**
 * Activity for Whisper Step-2 UI.
 * 
 * Loads the whisper-step2.html WebView with AndroidWhisper bridge.
 */
class WhisperStep2Activity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "WhisperStep2Activity"
    }
    
    private lateinit var webView: WebView
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "Creating WhisperStep2Activity")
        
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
            
            // Add the AndroidWhisper bridge
            addJavascriptInterface(AndroidWhisperBridge(this@WhisperStep2Activity), "AndroidWhisper")
            
            // Load the simplified whisper-step2.html file
            loadUrl("file:///android_asset/web/whisper-step2-simple.html")
        }
        
        setContentView(webView)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Destroying WhisperStep2Activity")
    }
}
