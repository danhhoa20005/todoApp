package com.example.appmanagement.data.dao

import androidx.room.*
import com.example.appmanagement.data.entity.User

// DAO cho bảng users
@Dao
interface UserDao {

    // Thêm user mới, trả về id
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: User): Long

    // Lấy user theo email
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): User?

    // Lấy user theo id
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): User?

    // Kiểm tra email đã tồn tại
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)")
    suspend fun existsEmail(email: String): Boolean

    // Cập nhật user
    @Update
    suspend fun update(user: User)

    // Xoá user
    @Delete
    suspend fun delete(user: User)
}
