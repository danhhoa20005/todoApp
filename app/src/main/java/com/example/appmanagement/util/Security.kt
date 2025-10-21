package com.example.appmanagement.util

import at.favre.lib.crypto.bcrypt.BCrypt

// Helpers for hashing and verifying sensitive credentials
object Security {
    // Computational cost factor used for bcrypt operations
    private const val COST = 12

    // Hash the provided password and clear the input array afterwards
    fun hashPassword(plain: CharArray): String {
        val hash = BCrypt.withDefaults().hashToString(COST, plain)
        plain.fill('\u0000')
        return hash
    }

    // Verify that the plain password matches the stored bcrypt hash
    fun verifyPassword(plain: CharArray, storedHash: String): Boolean {
        val r = BCrypt.verifyer().verify(plain, storedHash)
        plain.fill('\u0000')
        return r.verified
    }

    // Perform a lightweight check to see if the text resembles a bcrypt hash
    fun looksHashed(v: String): Boolean =
        v.startsWith("\$2a\$") || v.startsWith("\$2b\$") || v.startsWith("\$2y\$")
}
