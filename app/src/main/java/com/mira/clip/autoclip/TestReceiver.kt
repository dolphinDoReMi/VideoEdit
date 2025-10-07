package com.mira.clip.autoclip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Simple test receiver to verify broadcast mechanism works
 */
class TestReceiver : BroadcastReceiver() {

    init {
        Log.e("TestReceiver", "*** TEST RECEIVER CLASS INITIALIZED ***")
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.e("TestReceiver", "*** TEST RECEIVER CALLED *** Action: ${intent.action}")
    }

    companion object {
        const val ACTION_TEST = "com.mira.clip.TEST"
    }
}
