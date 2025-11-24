// Giao diện UserDao khai báo các truy vấn Room phục vụ quản lý người dùng và trạng thái đăng nhập
package com.example.appmanagement.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.appmanagement.data.entity.User

@Dao
interface UserDao {

    // ===== CREATE =====
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: User): Long

    // ===== READ =====
    // So khớp email không phân biệt hoa/thường
    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): User?

    // Quan sát user đang đăng nhập (LiveData)
    @Query("SELECT * FROM users WHERE is_logged_in = 1 LIMIT 1")
    fun observeLoggedInUser(): LiveData<User?>

    // Kiểm tra tồn tại email (không phân biệt hoa/thường)
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE LOWER(email) = LOWER(:email))")
    suspend fun existsEmail(email: String): Boolean

    // ===== UPDATE =====
    @Update
    suspend fun update(user: User): Int

    // Chỉ cập nhật các trường hồ sơ cơ bản
    @Query("""
        UPDATE users 
        SET name = :name, birth_date = :birthDate, avatar_url = :avatarUrl
        WHERE id = :id
    """)
    suspend fun updateProfile(
        id: Long,
        name: String,
        birthDate: String?,
        avatarUrl: String?
    ): Int

    // Đổi email (đã unique ở DB, nhớ handle lỗi UNIQUE ở repo)
    @Query("UPDATE users SET email = :newEmail WHERE id = :id")
    suspend fun updateEmail(id: Long, newEmail: String): Int

    // Cập nhật mật khẩu (đã băm BCrypt)
    @Query("UPDATE users SET password_hash = :newHash WHERE id = :id")
    suspend fun updatePasswordHash(id: Long, newHash: String): Int

    // ===== DELETE =====
    @Delete
    suspend fun delete(user: User): Int

    // ===== LOGIN STATE =====
    @Query("UPDATE users SET is_logged_in = 0")
    suspend fun clearLoggedIn(): Int

    @Query("UPDATE users SET is_logged_in = 1 WHERE id = :id")
    suspend fun setLoggedIn(id: Long): Int

    @Query("UPDATE users SET is_logged_in = 0 WHERE id = :id")
    suspend fun setLoggedOut(id: Long): Int

    @Query("SELECT * FROM users WHERE is_logged_in = 1 LIMIT 1")
    suspend fun getLoggedInUser(): User?

    // Gộp thao tác clear + set vào 1 transaction cho an toàn
    @Transaction
    suspend fun switchLoggedIn(newUserId: Long) {
        clearLoggedIn()
        setLoggedIn(newUserId)
    }
}
