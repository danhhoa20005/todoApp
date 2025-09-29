package com.example.appmanagement.data.repo

import com.example.appmanagement.data.dao.UserDao
import com.example.appmanagement.data.entity.User
import com.example.appmanagement.data.pref.UserPreferences
import kotlinx.coroutines.flow.first
import java.security.MessageDigest

/**
 * Repository: trung gian giữa ViewModel ↔ Room (UserDao) + DataStore (UserPreferences).
 * Xử lý nghiệp vụ: đăng ký, đăng nhập, đăng xuất, cập nhật hồ sơ.
 */
class AccountRepository(
    private val userDao: UserDao,
    private val prefs: UserPreferences
) {

    /** Đăng ký user mới */
    suspend fun register(
        name: String,
        email: String,
        password: String,
        birthDate: String = "",
        avatarUrl: String? = null
    ): Long {
        require(name.isNotBlank() && email.isNotBlank() && password.length >= 6) {
            "invalid_input"
        }
        if (userDao.existsEmail(email.trim())) throw IllegalStateException("email_exists")

        val user = User(
            name = name.trim(),
            email = email.trim(),
            passwordHash = sha256(password),
            birthDate = birthDate,
            avatarUrl = avatarUrl
        )
        val id = userDao.insert(user)

        prefs.setLoggedIn(true)
        prefs.setCurrentUserId(id)
        prefs.setUserInfo(user.name, user.email, user.avatarUrl ?: "", user.birthDate)

        return id
    }

    /** Đăng nhập, trả về true/false */
    suspend fun login(email: String, password: String): Boolean {
        val u = userDao.getByEmail(email.trim()) ?: return false
        val ok = (u.passwordHash == sha256(password))
        if (ok) {
            prefs.setLoggedIn(true)
            prefs.setCurrentUserId(u.id)
            prefs.setUserInfo(u.name, u.email, u.avatarUrl ?: "", u.birthDate)
        } else {
            prefs.setLoggedIn(false)
            prefs.setCurrentUserId(-1L)
        }
        return ok
    }

    /** Đăng xuất: xoá trạng thái trong DataStore */
    suspend fun logout() {
        prefs.clear()
    }

    /** Lấy user hiện tại từ DataStore + DB */
    suspend fun getCurrentUser(): User? {
        val id = prefs.currentUserIdFlow.first()
        return if (id > 0) userDao.getById(id) else null
    }

    /** Cập nhật hồ sơ user */
    suspend fun updateProfile(id: Long, name: String, birthDate: String, avatarUrl: String?) {
        val u = userDao.getById(id) ?: return
        val updated = u.copy(
            name = name.trim(),
            birthDate = birthDate,
            avatarUrl = avatarUrl
        )
        userDao.update(updated)
        prefs.setUserInfo(updated.name, updated.email, updated.avatarUrl ?: "", updated.birthDate)
    }

    /** Hash SHA-256 (demo, không dùng cho sản phẩm thật) */
    private fun sha256(x: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(x.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
