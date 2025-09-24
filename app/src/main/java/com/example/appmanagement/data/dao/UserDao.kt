package com.example.appmanagement.data.dao

import androidx.room.*
import com.example.appmanagement.data.entity.User


@Dao // DAO = nơi định nghĩa các thao tác CSDL (SQL) cho bảng User
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: User  ): Long
    // chèn người dùng mới; ABORT: nếu email trùng (do unique index) → ném lỗi

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): User?
    // lấy 1 user theo email (phục vụ đăng nhập/đăng ký)

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)")
    suspend fun existsEmail(email: String): Boolean
    // kiểm tra email đã tồn tại chưa (đăng ký)

    @Delete
    suspend fun delete(user: User)
    // xoá user (ít dùng cho đăng nhập, để sẵn)
}
