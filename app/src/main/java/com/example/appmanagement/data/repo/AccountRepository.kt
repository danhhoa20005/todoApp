package com.example.appmanagement.data.repo

import com.example.appmanagement.data.dao.UserDao
import com.example.appmanagement.data.entity.User
import com.example.appmanagement.util.Security

// Repository quản lý các thao tác tài khoản giữa ViewModel và UserDao
class AccountRepository(
    private val userDao: UserDao
) {

    // Đăng ký user mới với mật khẩu được băm và tự động đăng nhập
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

        // Nếu email đã tồn tại, báo lỗi
        if (userDao.existsEmail(email.trim())) {
            throw IllegalStateException("email_exists")
        }

        // Băm mật khẩu bằng BCrypt
        val hashedPassword = Security.hashPassword(password.toCharArray())

        val newUser = User(
            name = name.trim(),
            email = email.trim(),
            passwordHash = hashedPassword,
            birthDate = birthDate,
            avatarUrl = avatarUrl,
            isLoggedIn = true
        )

        // Xoá trạng thái đăng nhập trước và thêm người dùng mới
        userDao.clearLoggedIn()
        return userDao.insert(newUser)
    }

    // Đăng nhập bằng email và mật khẩu, trả về true nếu thành công
    suspend fun login(email: String, password: String): Boolean {
        val user = userDao.getByEmail(email.trim()) ?: return false

        val isVerified = when {
            Security.looksHashed(user.passwordHash) -> {
                // So sánh bằng BCrypt
                Security.verifyPassword(password.toCharArray(), user.passwordHash)
            }
            else -> {
                // Nếu dữ liệu cũ còn plaintext thì so sánh và nâng cấp lên hash
                val match = user.passwordHash == password
                if (match) {
                    val newHash = Security.hashPassword(password.toCharArray())
                    userDao.updatePasswordHash(user.id, newHash)
                }
                match
            }
        }

        if (isVerified) {
            userDao.clearLoggedIn()
            userDao.setLoggedIn(user.id)
        }

        return isVerified
    }

    // Đăng xuất bằng cách xóa trạng thái đăng nhập
    suspend fun logout() {
        userDao.clearLoggedIn()
    }

    // Lấy người dùng đang đăng nhập hiện tại
    suspend fun getCurrentUser(): User? {
        return userDao.getLoggedInUser()
    }

    // Cập nhật thông tin hồ sơ cơ bản
    suspend fun updateProfile(id: Long, name: String, birthDate: String?, avatarUrl: String?) {
        val user = userDao.getById(id) ?: return
        val updatedUser = user.copy(
            name = name.trim(),
            birthDate = birthDate,
            avatarUrl = avatarUrl
        )
        userDao.update(updatedUser)
    }

    // Đổi mật khẩu sau khi kiểm tra độ dài tối thiểu
    suspend fun changePassword(userId: Long, newPassword: String) {
        require(newPassword.length >= 6) { "short_password" }
        val hash = Security.hashPassword(newPassword.toCharArray())
        userDao.updatePasswordHash(userId, hash)
    }
}
