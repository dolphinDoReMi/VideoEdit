package com.mira.com.whisper

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class WhisperRunConsoleActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Add AndroidWhisper bridge for job listing
                    addJavascriptInterface(AndroidWhisperBridge(this@WhisperRunConsoleActivity), "AndroidWhisper")
                }
            }
            loadUrl("file:///android_asset/web/whisper-run-console.html")
        }

        setContentView(webView)
    }
}