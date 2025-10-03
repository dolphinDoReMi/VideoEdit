package com.mira.videoeditor

import android.app.Application
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util

class AutoCutApplication : Application() {
    
    companion object {
        lateinit var instance: AutoCutApplication
            private set
    }
    
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize Media3 user agent
        Util.getUserAgent(this, "Mira")
        
        // Initialize crash reporting (if needed)
        initializeCrashReporting()
        
        // Initialize analytics (if needed)
        initializeAnalytics()
    }
    
    private fun initializeCrashReporting() {
        // Initialize crash reporting service
        // Example: Firebase Crashlytics, Bugsnag, etc.
    }
    
    private fun initializeAnalytics() {
        // Initialize analytics service
        // Example: Firebase Analytics, Mixpanel, etc.
    }
    
    fun getAppContext(): Context = this
}
