package com.example.appmanagement.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object Crypto {
    private const val SALT_BYTES = 16

    fun hashPassword(plain: String, salt: ByteArray = genSalt()): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val digest = md.digest(plain.toByteArray())
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val digB64  = Base64.encodeToString(digest, Base64.NO_WRAP)
        return "$saltB64:$digB64"
    }

    fun verify(plain: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val salt = Base64.decode(parts[0], Base64.NO_WRAP)
        val expected = parts[1]
        return hashPassword(plain, salt).split(":")[1] == expected
    }

    private fun genSalt(): ByteArray {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        return salt
    }
}
