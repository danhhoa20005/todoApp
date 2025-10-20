package com.example.appmanagement.util

import at.favre.lib.crypto.bcrypt.BCrypt

object Security {
    private const val COST = 12

    fun hashPassword(plain: CharArray): String {
        val hash = BCrypt.withDefaults().hashToString(COST, plain)
        plain.fill('\u0000')
        return hash
    }

    fun verifyPassword(plain: CharArray, storedHash: String): Boolean {
        val r = BCrypt.verifyer().verify(plain, storedHash)
        plain.fill('\u0000')
        return r.verified
    }

    fun looksHashed(v: String): Boolean =
        v.startsWith("\$2a\$") || v.startsWith("\$2b\$") || v.startsWith("\$2y\$")
}
