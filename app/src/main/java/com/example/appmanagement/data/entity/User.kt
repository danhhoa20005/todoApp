package com.example.appmanagement.data.entity

import androidx.room.*

// Room entity storing a user's profile and authentication information
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(

    // Khóa chính tự tăng
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // Tên hiển thị của người dùng
    @ColumnInfo(name = "name")
    val name: String,

    // Email duy nhất dùng để đăng nhập
    @ColumnInfo(name = "email")
    val email: String,

    // Mật khẩu dạng băm sử dụng BCrypt
    @ColumnInfo(name = "password_hash")
    val passwordHash: String,

    // Ngày sinh có thể bỏ trống
    @ColumnInfo(name = "birth_date")
    val birthDate: String?,

    // Đường dẫn hoặc URL ảnh đại diện
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,

    // Thời điểm tạo tài khoản
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    // Trạng thái đăng nhập hiện tại
    @ColumnInfo(name = "is_logged_in")
    val isLoggedIn: Boolean = false
)
