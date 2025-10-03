package com.mira.com.feature.whisper.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update

@Entity(
    tableName = "asr_files",
    indices = [Index(value = ["uri"], unique = true)],
)
data class AsrFile(
    // hash(uri)
    @PrimaryKey val id: String,
    val uri: String,
    val mime: String?,
    val durationMs: Long?,
    val srHz: Int?,
    val channels: Int?,
    // NEW | QUEUED | DONE | ERROR
    val state: String,
    val updatedAtMs: Long,
)

@Entity(
    tableName = "asr_jobs",
    foreignKeys = [
        ForeignKey(
            entity = AsrFile::class,
            parentColumns = ["id"],
            childColumns = ["fileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("fileId"), Index("createdAtMs")],
)
data class AsrJob(
    @PrimaryKey val jobId: String,
    val fileId: String,
    val model: String,
    val threads: Int,
    val beam: Int,
    val lang: String,
    val translate: Boolean,
    val createdAtMs: Long,
    val inferMs: Long?,
    val rtf: Double?,
    val sidecarPath: String?,
    // RUNNING | DONE | ERROR
    val status: String,
    val error: String?,
)

@Entity(
    tableName = "asr_segments",
    foreignKeys = [
        ForeignKey(
            entity = AsrJob::class,
            parentColumns = ["jobId"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("jobId")],
)
data class AsrSegment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: String,
    val t0Ms: Long,
    val t1Ms: Long,
    val text: String,
)

@Dao
interface AsrDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun upsertFile(file: AsrFile): Long

    @Update fun updateFile(file: AsrFile)

    @Query("SELECT * FROM asr_files WHERE state=:state LIMIT :limit")
    fun filesByState(
        state: String,
        limit: Int,
    ): List<AsrFile>

    @Insert fun insertJob(job: AsrJob)

    @Query("UPDATE asr_jobs SET inferMs=:inferMs, rtf=:rtf, status=:status, sidecarPath=:sidecarPath, error=:error WHERE jobId=:jobId")
    fun finishJob(
        jobId: String,
        inferMs: Long?,
        rtf: Double?,
        status: String,
        sidecarPath: String?,
        error: String?,
    )

    @Insert fun insertSegments(segments: List<AsrSegment>)
}

@Database(entities = [AsrFile::class, AsrJob::class, AsrSegment::class], version = 1)
abstract class AsrDatabase : RoomDatabase() {
    abstract fun dao(): AsrDao
}
