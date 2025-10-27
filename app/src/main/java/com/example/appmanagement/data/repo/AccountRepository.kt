package com.example.appmanagement.data.repo

import com.example.appmanagement.data.dao.UserDao
import com.example.appmanagement.data.entity.User
import com.example.appmanagement.util.Security

/**
 * Lớp Repository trung gian giữa ViewModel ↔ Room (UserDao)
 * Dùng để quản lý tài khoản người dùng:
 * - Đăng ký, đăng nhập, đăng xuất
 * - Cập nhật hồ sơ, đổi mật khẩu
 * Mật khẩu luôn được lưu dưới dạng mã băm (hash) bằng BCrypt.
 */
class AccountRepository(
    private val userDao: UserDao     // DAO giao tiếp trực tiếp với bảng User trong Room DB
) {

    /** Đăng ký tài khoản mới (mật khẩu được băm trước khi lưu) */
    suspend fun register(
        name: String,
        email: String,
        password: String,
        birthDate: String = "",
        avatarUrl: String? = null
    ): Long {
        // Kiểm tra hợp lệ: tên, email, và mật khẩu ≥ 6 ký tự
        require(name.isNotBlank() && email.isNotBlank() && password.length >= 6) {
            "invalid_input"
        }

        // Nếu email đã tồn tại trong DB → báo lỗi
        if (userDao.existsEmail(email.trim())) {
            throw IllegalStateException("email_exists")
        }

        // Băm mật khẩu bằng BCrypt → tạo chuỗi mã hóa an toàn
        val hashedPassword = Security.hashPassword(password.toCharArray())

        // Tạo đối tượng User mới
        val newUser = User(
            name = name.trim(),
            email = email.trim(),
            passwordHash = hashedPassword,
            birthDate = birthDate,
            avatarUrl = avatarUrl,
            isLoggedIn = true   // Sau khi đăng ký thì tự động đăng nhập
        )

        // Đảm bảo chỉ có 1 user đang đăng nhập → clear user cũ
        userDao.clearLoggedIn()

        // Thêm user mới vào database và trả về ID
        return userDao.insert(newUser)
    }

    /** Đăng nhập: trả về true nếu thành công */
    suspend fun login(email: String, password: String): Boolean {
        // Tìm user theo email (nếu không có thì trả về false)
        val user = userDao.getByEmail(email.trim()) ?: return false

        // Kiểm tra mật khẩu
        val isVerified = when {
            Security.looksHashed(user.passwordHash) -> {
                // Nếu là hash BCrypt → so sánh bằng cách verify
                Security.verifyPassword(password.toCharArray(), user.passwordHash)
            }
            else -> {
                // Nếu dữ liệu cũ lưu plaintext (chưa mã hóa)
                // So sánh trực tiếp, rồi tự động nâng cấp thành hash mới
                val match = user.passwordHash == password
                if (match) {
                    val newHash = Security.hashPassword(password.toCharArray())
                    userDao.updatePasswordHash(user.id, newHash)
                }
                match
            }
        }

        // Nếu mật khẩu đúng → clear user khác và set user này là đang đăng nhập
        if (isVerified) {
            userDao.clearLoggedIn()
            userDao.setLoggedIn(user.id)
        }

        return isVerified
    }

    /** Đăng xuất: xóa trạng thái login của tất cả user */
    suspend fun logout() {
        userDao.clearLoggedIn()
    }

    /** Lấy thông tin user hiện đang đăng nhập (nếu có) */
    suspend fun getCurrentUser(): User? {
        return userDao.getLoggedInUser()
    }

    /** Cập nhật hồ sơ người dùng (tên, ngày sinh, avatar) */
    suspend fun updateProfile(id: Long, name: String, birthDate: String?, avatarUrl: String?) {
        val user = userDao.getById(id) ?: return    // Không tìm thấy thì bỏ qua
        val updatedUser = user.copy(
            name = name.trim(),
            birthDate = birthDate,
            avatarUrl = avatarUrl
        )
        userDao.update(updatedUser)  // Cập nhật lại trong DB
    }

    /** Đổi mật khẩu người dùng hiện tại */
    suspend fun changePassword(userId: Long, newPassword: String) {
        require(newPassword.length >= 6) { "short_password" }   // Mật khẩu phải ≥ 6 ký tự
        val hash = Security.hashPassword(newPassword.toCharArray())
        userDao.updatePasswordHash(userId, hash)
    }
}
