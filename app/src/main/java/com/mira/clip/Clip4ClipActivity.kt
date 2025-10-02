package com.mira.clip

import android.app.Activity
import android.os.Bundle
import android.util.Log

/**
 * Simple launcher activity for mira_clip CLIP4Clip service.
 * 
 * This activity just logs that the app has been launched and exits.
 * The main functionality is in the broadcast receivers.
 */
class Clip4ClipActivity : Activity() {
    
    companion object {
        private const val TAG = "Clip4ClipActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.i(TAG, "CLIP4Clip app launched - broadcast receivers are now active")
        
        // Just finish immediately - this is a headless service
        finish()
    }
}
