package com.example.appmanagement.data.repo

import com.example.appmanagement.data.dao.UserDao
import com.example.appmanagement.data.entity.User

/**
 * Repository: trung gian giữa ViewModel ↔ Room (UserDao).
 * Xử lý nghiệp vụ: đăng ký, đăng nhập, đăng xuất, cập nhật hồ sơ.
 * Không còn dùng DataStore, trạng thái login được lưu trong Room (cột is_logged_in).
 */
class AccountRepository(
    private val userDao: UserDao
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
            passwordHash = password,
            birthDate = birthDate,
            avatarUrl = avatarUrl,
            isLoggedIn = true
        )

        // Clear các user khác đang login rồi mới insert user mới
        userDao.clearLoggedIn()
        return userDao.insert(user)
    }

    /** Đăng nhập, trả về true/false */
    suspend fun login(email: String, password: String): Boolean {
        val u = userDao.getByEmail(email.trim()) ?: return false
        val ok = (u.passwordHash == password) // so sánh plain text
        if (ok) {
            userDao.clearLoggedIn()
            userDao.setLoggedIn(u.id)
        }
        return ok
    }

    /** Đăng xuất: clear trạng thái login */
    suspend fun logout() {
        userDao.clearLoggedIn()
    }

    /** Lấy user hiện tại */
    suspend fun getCurrentUser(): User? {
        return userDao.getLoggedInUser()
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
    }
}
