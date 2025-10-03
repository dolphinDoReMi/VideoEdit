package com.mira.com.feature.whisper.data.db

import android.content.Context
import androidx.room.Room

object AsrDb {
    @Volatile private var instance: AsrDatabase? = null

    fun get(ctx: Context): AsrDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                ctx.applicationContext,
                AsrDatabase::class.java, "asr.db",
            ).build().also { instance = it }
        }
}
