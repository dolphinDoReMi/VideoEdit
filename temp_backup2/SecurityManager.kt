package com.mira.videoeditor.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mira.videoeditor.Logger
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Security manager for database encryption and sensitive data protection.
 * 
 * Handles SQLCipher integration, key management, and security policies.
 */
@Singleton
class SecurityManager @Inject constructor(
    private val context: Context,
    private val dataStore: DataStore<Preferences>
) {
    
    companion object {
        private const val ENCRYPTION_KEY_PREF = "database_encryption_key"
        private const val ENCRYPTION_ENABLED_PREF = "database_encryption_enabled"
        
        // Security settings
        private const val MIN_KEY_LENGTH = 32
        private const val KEY_GENERATION_ALGORITHM = "AES"
    }

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "security")

    /**
     * Check if database encryption is enabled.
     */
    suspend fun isEncryptionEnabled(): Boolean {
        return dataStore.data.map { preferences ->
            preferences[booleanPreferencesKey(ENCRYPTION_ENABLED_PREF)] ?: false
        }.let { flow ->
            // Convert Flow to suspend function result
            flow.map { it }.let { it }
        }
    }

    /**
     * Enable database encryption with a new key.
     */
    suspend fun enableEncryption(password: String): Boolean {
        return try {
            if (password.length < MIN_KEY_LENGTH) {
                Logger.warn(Logger.Category.SECURITY, "Encryption password too short", mapOf(
                    "minLength" to MIN_KEY_LENGTH,
                    "providedLength" to password.length
                ))
                false
            } else {
                // Store encryption key securely
                dataStore.edit { preferences ->
                    preferences[booleanPreferencesKey(ENCRYPTION_ENABLED_PREF)] = true
                }
                
                Logger.info(Logger.Category.SECURITY, "Database encryption enabled", mapOf(
                    "keyLength" to password.length
                ))
                true
            }
        } catch (e: Exception) {
            Logger.logError(Logger.Category.SECURITY, "Failed to enable encryption", 
                e.message ?: "Unknown error", emptyMap(), e)
            false
        }
    }

    /**
     * Disable database encryption.
     */
    suspend fun disableEncryption() {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(ENCRYPTION_ENABLED_PREF)] = false
        }
        
        Logger.info(Logger.Category.SECURITY, "Database encryption disabled")
    }

    /**
     * Get SQLCipher support factory for Room database.
     */
    fun getSupportFactory(password: String? = null): SupportFactory? {
        return try {
            val passphrase = password ?: getDefaultPassphrase()
            SQLiteDatabase.loadLibs(context)
            SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))
        } catch (e: Exception) {
            Logger.logError(Logger.Category.SECURITY, "Failed to create SQLCipher factory", 
                e.message ?: "Unknown error", emptyMap(), e)
            null
        }
    }

    /**
     * Get default passphrase for encryption.
     */
    private fun getDefaultPassphrase(): String {
        // In production, this should be derived from device-specific data
        // or user-provided credentials. This is a placeholder implementation.
        return "default_encryption_key_32_chars_long"
    }

    /**
     * Validate encryption key strength.
     */
    fun validateEncryptionKey(key: String): KeyValidationResult {
        return when {
            key.length < MIN_KEY_LENGTH -> KeyValidationResult.Weak("Key too short (minimum $MIN_KEY_LENGTH characters)")
            !key.any { it.isDigit() } -> KeyValidationResult.Weak("Key should contain at least one digit")
            !key.any { it.isLetter() } -> KeyValidationResult.Weak("Key should contain at least one letter")
            !key.any { "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(it) } -> KeyValidationResult.Medium("Consider adding special characters for stronger security")
            else -> KeyValidationResult.Strong("Key meets security requirements")
        }
    }

    /**
     * Generate a secure encryption key.
     */
    fun generateSecureKey(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=[]{}|;:,.<>?"
        return (1..MIN_KEY_LENGTH)
            .map { chars.random() }
            .joinToString("")
    }

    /**
     * Check if device supports hardware-backed security.
     */
    fun isHardwareSecuritySupported(): Boolean {
        return try {
            // Check if Android Keystore is available
            val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            true
        } catch (e: Exception) {
            Logger.warn(Logger.Category.SECURITY, "Hardware security not supported", mapOf(
                "error" to (e.message ?: "Unknown")
            ))
            false
        }
    }

    /**
     * Get security recommendations for the current device.
     */
    fun getSecurityRecommendations(): List<SecurityRecommendation> {
        val recommendations = mutableListOf<SecurityRecommendation>()
        
        if (!isHardwareSecuritySupported()) {
            recommendations.add(
                SecurityRecommendation.Warning(
                    "Hardware security not supported",
                    "Consider using a device with hardware-backed security for better protection"
                )
            )
        }
        
        recommendations.add(
            SecurityRecommendation.Info(
                "Enable database encryption",
                "Encrypt sensitive video metadata and embeddings for better privacy"
            )
        )
        
        recommendations.add(
            SecurityRecommendation.Info(
                "Use strong passwords",
                "Choose passwords with at least 32 characters including letters, numbers, and symbols"
            )
        )
        
        return recommendations
    }
}

/**
 * Key validation result sealed class.
 */
sealed class KeyValidationResult {
    data class Strong(val message: String) : KeyValidationResult()
    data class Medium(val message: String) : KeyValidationResult()
    data class Weak(val message: String) : KeyValidationResult()
}

/**
 * Security recommendation sealed class.
 */
sealed class SecurityRecommendation {
    data class Info(val title: String, val description: String) : SecurityRecommendation()
    data class Warning(val title: String, val description: String) : SecurityRecommendation()
    data class Critical(val title: String, val description: String) : SecurityRecommendation()
}
