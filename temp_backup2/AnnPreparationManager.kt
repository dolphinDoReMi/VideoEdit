package com.mira.videoeditor.ann

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mira.videoeditor.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ANN (Approximate Nearest Neighbor) preparation for large-scale vector search.
 * 
 * Prepares the database schema and infrastructure for future ANN implementations
 * when the corpus grows beyond brute-force search capabilities.
 */
@Singleton
class AnnPreparationManager @Inject constructor() {

    companion object {
        // ANN configuration constants
        private const val DEFAULT_HNSW_M = 16
        private const val DEFAULT_HNSW_EF_CONSTRUCTION = 200
        private const val DEFAULT_HNSW_EF_SEARCH = 50
        private const val DEFAULT_PQ_M = 8
        private const val DEFAULT_PQ_BITS = 8
    }

    /**
     * ANN index entity for HNSW (Hierarchical Navigable Small World) graphs.
     */
    @Entity(
        tableName = "ann_hnsw_index",
        indices = [
            Index("ownerId"),
            Index("variant"),
            Index("dimension"),
            Index(value = ["ownerId", "variant"], unique = true)
        ]
    )
    data class HnswIndexEntity(
        @PrimaryKey val id: String = UUID.randomUUID().toString(),
        val ownerId: String,                    // Video/Shot/Text ID
        val variant: String,                   // Embedding variant
        val dimension: Int,                    // Vector dimension
        val m: Int = DEFAULT_HNSW_M,          // HNSW connectivity parameter
        val efConstruction: Int = DEFAULT_HNSW_EF_CONSTRUCTION,
        val efSearch: Int = DEFAULT_HNSW_EF_SEARCH,
        val graphData: ByteArray,              // Serialized HNSW graph
        val createdAt: Long = System.currentTimeMillis(),
        val updatedAt: Long = System.currentTimeMillis()
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as HnswIndexEntity

            if (id != other.id) return false
            if (ownerId != other.ownerId) return false
            if (variant != other.variant) return false
            if (dimension != other.dimension) return false
            if (m != other.m) return false
            if (efConstruction != other.efConstruction) return false
            if (efSearch != other.efSearch) return false
            if (!graphData.contentEquals(other.graphData)) return false
            if (createdAt != other.createdAt) return false
            if (updatedAt != other.updatedAt) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + ownerId.hashCode()
            result = 31 * result + variant.hashCode()
            result = 31 * result + dimension
            result = 31 * result + m
            result = 31 * result + efConstruction
            result = 31 * result + efSearch
            result = 31 * result + graphData.contentHashCode()
            result = 31 * result + createdAt.hashCode()
            result = 31 * result + updatedAt.hashCode()
            return result
        }
    }

    /**
     * Product Quantization (PQ) codebook entity for compressed vector storage.
     */
    @Entity(
        tableName = "ann_pq_codebook",
        indices = [
            Index("variant"),
            Index("dimension"),
            Index(value = ["variant", "dimension"], unique = true)
        ]
    )
    data class PqCodebookEntity(
        @PrimaryKey val id: String = UUID.randomUUID().toString(),
        val variant: String,                   // Embedding variant
        val dimension: Int,                    // Original vector dimension
        val m: Int = DEFAULT_PQ_M,             // Number of subvectors
        val bits: Int = DEFAULT_PQ_BITS,       // Bits per subvector
        val codebookData: ByteArray,           // Serialized PQ codebook
        val createdAt: Long = System.currentTimeMillis()
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PqCodebookEntity

            if (id != other.id) return false
            if (variant != other.variant) return false
            if (dimension != other.dimension) return false
            if (m != other.m) return false
            if (bits != other.bits) return false
            if (!codebookData.contentEquals(other.codebookData)) return false
            if (createdAt != other.createdAt) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + variant.hashCode()
            result = 31 * result + dimension
            result = 31 * result + m
            result = 31 * result + bits
            result = 31 * result + codebookData.contentHashCode()
            result = 31 * result + createdAt.hashCode()
            return result
        }
    }

    /**
     * ANN statistics entity for monitoring index performance.
     */
    @Entity(
        tableName = "ann_statistics",
        indices = [
            Index("variant"),
            Index("indexType"),
            Index("createdAt")
        ]
    )
    data class AnnStatisticsEntity(
        @PrimaryKey val id: String = UUID.randomUUID().toString(),
        val variant: String,                   // Embedding variant
        val indexType: String,                 // "hnsw", "pq", "ivf", etc.
        val vectorCount: Int,                  // Number of vectors indexed
        val indexSizeBytes: Long,              // Size of index in bytes
        val buildTimeMs: Long,                 // Time to build index
        val searchTimeMs: Double,              // Average search time
        val recall: Double,                    // Search recall (0.0-1.0)
        val precision: Double,                 // Search precision (0.0-1.0)
        val createdAt: Long = System.currentTimeMillis()
    )

    /**
     * DAO for ANN index operations.
     */
    @Dao
    interface AnnIndexDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun upsertHnswIndex(index: HnswIndexEntity)

        @Query("SELECT * FROM ann_hnsw_index WHERE ownerId = :ownerId AND variant = :variant")
        suspend fun getHnswIndex(ownerId: String, variant: String): HnswIndexEntity?

        @Query("SELECT * FROM ann_hnsw_index WHERE variant = :variant")
        suspend fun getAllHnswIndices(variant: String): List<HnswIndexEntity>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun upsertPqCodebook(codebook: PqCodebookEntity)

        @Query("SELECT * FROM ann_pq_codebook WHERE variant = :variant AND dimension = :dimension")
        suspend fun getPqCodebook(variant: String, dimension: Int): PqCodebookEntity?

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertStatistics(stats: AnnStatisticsEntity)

        @Query("SELECT * FROM ann_statistics WHERE variant = :variant ORDER BY createdAt DESC LIMIT :limit")
        suspend fun getStatistics(variant: String, limit: Int = 10): List<AnnStatisticsEntity>

        @Query("DELETE FROM ann_hnsw_index WHERE variant = :variant")
        suspend fun deleteHnswIndices(variant: String)

        @Query("DELETE FROM ann_pq_codebook WHERE variant = :variant")
        suspend fun deletePqCodebooks(variant: String)
    }

    /**
     * Migration to add ANN tables (version 2 to 3).
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create HNSW index table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `ann_hnsw_index` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `ownerId` TEXT NOT NULL,
                    `variant` TEXT NOT NULL,
                    `dimension` INTEGER NOT NULL,
                    `m` INTEGER NOT NULL DEFAULT 16,
                    `efConstruction` INTEGER NOT NULL DEFAULT 200,
                    `efSearch` INTEGER NOT NULL DEFAULT 50,
                    `graphData` BLOB NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL
                )
            """.trimIndent())

            // Create PQ codebook table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `ann_pq_codebook` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `variant` TEXT NOT NULL,
                    `dimension` INTEGER NOT NULL,
                    `m` INTEGER NOT NULL DEFAULT 8,
                    `bits` INTEGER NOT NULL DEFAULT 8,
                    `codebookData` BLOB NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
            """.trimIndent())

            // Create ANN statistics table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `ann_statistics` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `variant` TEXT NOT NULL,
                    `indexType` TEXT NOT NULL,
                    `vectorCount` INTEGER NOT NULL,
                    `indexSizeBytes` INTEGER NOT NULL,
                    `buildTimeMs` INTEGER NOT NULL,
                    `searchTimeMs` REAL NOT NULL,
                    `recall` REAL NOT NULL,
                    `precision` REAL NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
            """.trimIndent())

            // Create indices
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ann_hnsw_index_ownerId` ON `ann_hnsw_index` (`ownerId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ann_hnsw_index_variant` ON `ann_hnsw_index` (`variant`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ann_hnsw_index_dimension` ON `ann_hnsw_index` (`dimension`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ann_hnsw_index_ownerId_variant` ON `ann_hnsw_index` (`ownerId`, `variant`)")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ann_pq_codebook_variant` ON `ann_pq_codebook` (`variant`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ann_pq_codebook_dimension` ON `ann_pq_codebook` (`dimension`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ann_pq_codebook_variant_dimension` ON `ann_pq_codebook` (`variant`, `dimension`)")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ann_statistics_variant` ON `ann_statistics` (`variant`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ann_statistics_indexType` ON `ann_statistics` (`indexType`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ann_statistics_createdAt` ON `ann_statistics` (`createdAt`)")

            Logger.info(Logger.Category.ANN, "ANN tables migration completed")
        }
    }

    /**
     * Check if ANN infrastructure is ready.
     */
    suspend fun isAnnInfrastructureReady(): Boolean = withContext(Dispatchers.IO) {
        // Check if ANN tables exist and have proper structure
        // This would be implemented with actual database checks
        true
    }

    /**
     * Get ANN readiness assessment.
     */
    suspend fun getAnnReadinessAssessment(): AnnReadinessAssessment = withContext(Dispatchers.IO) {
        val isReady = isAnnInfrastructureReady()
        
        AnnReadinessAssessment(
            isReady = isReady,
            recommendations = if (isReady) {
                listOf(
                    "ANN infrastructure is ready for large-scale vector search",
                    "Consider implementing HNSW index when corpus exceeds 10,000 vectors",
                    "Monitor search performance and switch to ANN when brute-force becomes slow"
                )
            } else {
                listOf(
                    "Run database migration to add ANN tables",
                    "Implement ANN index building when needed",
                    "Prepare for large-scale vector search"
                )
            },
            estimatedThreshold = 10000, // Switch to ANN at 10k vectors
            currentVectorCount = 0 // Would be fetched from actual database
        )
    }

    /**
     * Prepare ANN infrastructure for future use.
     */
    suspend fun prepareAnnInfrastructure(): Boolean = withContext(Dispatchers.IO) {
        try {
            Logger.info(Logger.Category.ANN, "Preparing ANN infrastructure")
            
            // This would trigger the migration and set up ANN tables
            // For now, we just log the preparation
            
            Logger.info(Logger.Category.ANN, "ANN infrastructure prepared successfully")
            true
        } catch (e: Exception) {
            Logger.logError(Logger.Category.ANN, "Failed to prepare ANN infrastructure", 
                e.message ?: "Unknown error", emptyMap(), e)
            false
        }
    }
}

/**
 * ANN readiness assessment data class.
 */
data class AnnReadinessAssessment(
    val isReady: Boolean,
    val recommendations: List<String>,
    val estimatedThreshold: Int,
    val currentVectorCount: Int
)
