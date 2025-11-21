// Quản lý tài khoản: đăng ký, đăng nhập, đăng xuất, cập nhật hồ sơ
package com.example.appmanagement.data.repo

import com.example.appmanagement.data.dao.UserDao
import com.example.appmanagement.data.entity.User
import com.example.appmanagement.util.Security

data class GoogleLoginResult(
    val user: User,
    val isExisting: Boolean
)

class AccountRepository(
    private val userDao: UserDao
) {

    // register – tạo user mới – hash mật khẩu – set đăng nhập – lưu Room
    suspend fun register(
        name: String,
        email: String,
        password: String,
        birthDate: String = "",
        avatarUrl: String? = null
    ): Long {
        require(name.isNotBlank() && email.isNotBlank() && password.length >= 6)

        if (userDao.existsEmail(email.trim())) {
            throw IllegalStateException("email_exists")
        }

        val hashedPassword = Security.hashPassword(password.toCharArray())

        val newUser = User(
            name = name.trim(),
            email = email.trim(),
            passwordHash = hashedPassword,
            birthDate = birthDate,
            avatarUrl = avatarUrl,
            isLoggedIn = true
        )

        userDao.clearLoggedIn()
        return userDao.insert(newUser)
    }

    // login – xác thực email + password – set đăng nhập – không liên quan Google
    suspend fun login(email: String, password: String): Boolean {
        val user = userDao.getByEmail(email.trim()) ?: return false

        val isVerified = when {
            Security.looksHashed(user.passwordHash) ->
                Security.verifyPassword(password.toCharArray(), user.passwordHash)

            else -> {
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

    // loginWithGoogleAccount – đăng nhập Google – match theo remoteId – tạo hoặc cập nhật user
    suspend fun loginWithGoogleAccount(
        uid: String,
        email: String?,
        name: String?,
        avatarUrl: String?
    ): GoogleLoginResult {
        val safeEmail = (email ?: "$uid@firebase.local").trim()
        val safeName = (name ?: safeEmail.substringBefore("@")).trim()

        val existing = userDao.getByRemoteId(uid)

        userDao.clearLoggedIn()

        return if (existing != null) {
            // user Google đã từng login -> cập nhật là đủ
            val updated = existing.copy(
                name = safeName,
                email = safeEmail,
                avatarUrl = avatarUrl,
                isLoggedIn = true,
                remoteId = uid,
                updatedAt = System.currentTimeMillis()
            )
            userDao.update(updated)
            GoogleLoginResult(updated, true)

        } else {
            // lần đầu đăng nhập Google -> tạo user mới
            val fakePassword = uid + "#google"
            val hashedPassword = Security.hashPassword(fakePassword.toCharArray())

            val newUser = User(
                name = safeName,
                email = safeEmail,
                passwordHash = hashedPassword,
                birthDate = null,
                avatarUrl = avatarUrl,
                isLoggedIn = true,
                remoteId = uid,
                updatedAt = System.currentTimeMillis()
            )
            val newId = userDao.insert(newUser)
            GoogleLoginResult(newUser.copy(id = newId), false)
        }
    }

    // logout – xóa trạng thái đăng nhập của toàn bộ user
    suspend fun logout() {
        userDao.clearLoggedIn()
    }

    // getCurrentUser – lấy user đang đăng nhập – phục vụ tự động login
    suspend fun getCurrentUser(): User? {
        return userDao.getLoggedInUser()
    }

    // updateProfile – cập nhật tên, ngày sinh, avatar – không động mật khẩu
    suspend fun updateProfile(id: Long, name: String, birthDate: String?, avatarUrl: String?) {
        val user = userDao.getById(id) ?: return
        val updatedUser = user.copy(
            name = name.trim(),
            birthDate = birthDate,
            avatarUrl = avatarUrl,
            updatedAt = System.currentTimeMillis()
        )
        userDao.update(updatedUser)
    }

    // changePassword – đổi mật khẩu local – hash lại bằng BCrypt
    suspend fun changePassword(userId: Long, newPassword: String) {
        require(newPassword.length >= 6)
        val hash = Security.hashPassword(newPassword.toCharArray())
        userDao.updatePasswordHash(userId, hash)
    }
}
