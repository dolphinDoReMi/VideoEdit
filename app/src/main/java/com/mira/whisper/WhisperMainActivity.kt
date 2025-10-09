package com.mira.whisper

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.activity.ComponentActivity

class WhisperMainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var androidWhisperBridge: AndroidWhisperBridge

    companion object {
        private const val TAG = "WhisperMainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "Whisper Main Activity launched")

        // Initialize WebView
        webView = WebView(this)
        setContentView(webView)

        // Configure WebView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true // Enable DOM storage for local storage

        // Initialize AndroidWhisperBridge and inject into WebView
        androidWhisperBridge = AndroidWhisperBridge(this)
        androidWhisperBridge.setActivity(this)
        androidWhisperBridge.setWebView(webView)
        webView.addJavascriptInterface(androidWhisperBridge, "AndroidWhisperBridge")

        // Load the local HTML file
        webView.loadUrl("file:///android_asset/whisper_ui.html")
    }
}