package com.mira.videoeditor.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context

/**
 * Room database for CLIP4Clip application
 */
@Database(
    entities = [
        VideoEntity::class,
        ShotEntity::class,
        TextEntity::class,
        EmbeddingEntity::class,
        ReadModelsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(VectorConverters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun videoDao(): VideoDao
    abstract fun shotDao(): ShotDao
    abstract fun textDao(): TextDao
    abstract fun embeddingDao(): EmbeddingDao
    abstract fun readModelsDao(): ReadModelsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "clip4clip_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
