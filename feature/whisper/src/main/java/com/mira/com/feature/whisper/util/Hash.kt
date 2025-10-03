package com.mira.com.feature.whisper.util
import java.security.MessageDigest

object Hash {
    fun sha1(s: String): String {
        val d = MessageDigest.getInstance("SHA-1").digest(s.toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }
}
