package com.mira.videoeditor.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database configuration for CLIP4Clip video-text retrieval system.
 * 
 * This database stores video metadata, shot boundaries, text queries,
 * and vector embeddings for semantic search functionality.
 */
@Database(
    entities = [
        VideoEntity::class,
        ShotEntity::class,
        TextEntity::class,
        EmbeddingEntity::class,
        TextFts::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(VectorConverters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun videoDao(): VideoDao
    abstract fun shotDao(): ShotDao
    abstract fun textDao(): TextDao
    abstract fun embeddingDao(): EmbeddingDao
    abstract fun readModelsDao(): ReadModelsDao
    abstract fun textFtsDao(): TextFtsDao

    companion object {
        @Volatile 
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "videoedit.db"
                )
                .addMigrations(MIGRATION_1_2)
                .addCallback(DatabaseCallback())
                .build()
                .also { INSTANCE = it }
            }

        /**
         * Migration from version 1 to 2: Add FTS table for text search
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create FTS virtual table for text search
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS `texts_fts`
                    USING FTS4(`text`, content=`texts`);
                """.trimIndent())
                
                // Populate FTS table with existing text data
                db.execSQL("INSERT INTO texts_fts(rowid, text) SELECT rowid, text FROM texts;")
            }
        }

        /**
         * Database callback for initialization and cleanup
         */
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Database created - can add initial data here if needed
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Database opened - can add maintenance tasks here
            }
        }
    }
}