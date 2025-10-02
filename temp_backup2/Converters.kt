package com.mira.videoeditor.db

import androidx.room.TypeConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * TypeConverters for Room database to handle complex data types.
 * 
 * These converters handle serialization of FloatArray to ByteArray
 * using little-endian byte order for consistency with PyTorch Mobile.
 */
object VectorConverters {
    
    /**
     * Convert FloatArray to ByteArray using little-endian byte order.
     * This ensures compatibility with PyTorch Mobile tensor serialization.
     */
    @TypeConverter
    @JvmStatic
    fun floatArrayToBlob(a: FloatArray?): ByteArray? {
        if (a == null) return null
        val bb = ByteBuffer.allocate(a.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        a.forEach { bb.putFloat(it) }
        return bb.array()
    }

    /**
     * Convert ByteArray back to FloatArray using little-endian byte order.
     */
    @TypeConverter
    @JvmStatic
    fun blobToFloatArray(b: ByteArray?): FloatArray? {
        if (b == null) return null
        val bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
        val out = FloatArray(b.size / 4)
        for (i in out.indices) out[i] = bb.getFloat()
        return out
    }
    
    /**
     * Utility function to convert FloatArray to little-endian ByteArray.
     * Used directly in repository layer for embedding storage.
     */
    fun floatArrayToLeBytes(a: FloatArray): ByteArray {
        val bb = ByteBuffer.allocate(a.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        a.forEach { bb.putFloat(it) }
        return bb.array()
    }
    
    /**
     * Utility function to convert little-endian ByteArray to FloatArray.
     * Used directly in repository layer for embedding retrieval.
     */
    fun leBytesToFloatArray(b: ByteArray): FloatArray {
        val bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
        val out = FloatArray(b.size / 4)
        for (i in out.indices) out[i] = bb.getFloat()
        return out
    }
}