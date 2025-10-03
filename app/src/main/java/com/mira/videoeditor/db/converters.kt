package com.mira.videoeditor.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Type converters for Room database
 */
class VectorConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromFloatArray(value: FloatArray?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toFloatArray(value: String?): FloatArray? {
        return value?.let {
            val listType = object : TypeToken<FloatArray>() {}.type
            gson.fromJson(it, listType)
        }
    }
}
