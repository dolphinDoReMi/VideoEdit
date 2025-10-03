package mira.ui

import android.app.Application
import android.content.Context

class AutoCutApplication : Application() {
    
    companion object {
        lateinit var instance: AutoCutApplication
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    fun getAppContext(): Context = this
}
