package com.example.appmanagement.data.repo

import com.example.appmanagement.data.dao.UserDao
import com.example.appmanagement.data.entity.User
import com.example.appmanagement.data.pref.UserPreferences
import java.security.MessageDigest

// Repository: xử lý nghiệp vụ tài khoản, nối ViewModel ↔ Room + DataStore
class AccountRepository(
    private val userDao: UserDao,
    private val prefs: UserPreferences
) {
    // Đăng ký
    suspend fun register(name: String, email: String, password: String): Long {
        require(name.isNotBlank() && email.isNotBlank() && password.length >= 6) {
            "invalid_input"
        }
        if (userDao.existsEmail(email.trim())) throw IllegalStateException("email_exists")

        val user = User(
            name = name.trim(),
            email = email.trim(),
            passwordHash = sha256(password)
        )
        val id = userDao.insert(user)

        // lưu trạng thái sau khi đăng ký (tùy yêu cầu)
        prefs.setLoggedIn(true)
        prefs.setCurrentUserId(id)
        return id
    }

    // Đăng nhập
    suspend fun login(email: String, password: String): Boolean {
        val u = userDao.getByEmail(email.trim()) ?: return false
        val ok = (u.passwordHash == sha256(password))
        if (ok) {
            prefs.setLoggedIn(true)
            prefs.setCurrentUserId(u.id)
        } else {
            prefs.setLoggedIn(false)
            prefs.setCurrentUserId(-1L)
        }
        return ok
    }

    // Đăng xuất
    suspend fun logout() {
        prefs.clear()
    }

    // SHA-256 demo (sản phẩm thật nên dùng bcrypt/argon2 + salt)
    private fun sha256(x: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(x.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
