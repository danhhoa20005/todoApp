package com.example.appmanagement.data.entity

import androidx.room.*

// Entity = mô tả 1 bảng trong Room (bảng "users")
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)] // ép email không trùng
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,                // khóa chính tự tăng

    @ColumnInfo(name = "name")
    val name: String,                 // tên hiển thị

    @ColumnInfo(name = "email")
    val email: String,                // email đăng nhập (unique)

    @ColumnInfo(name = "password_hash")
    val passwordHash: String,         // mật khẩu sau khi băm

    @ColumnInfo(name = "birth_date")
    val birthDate: String,            // ngày sinh (ví dụ: "02/05/2005")

    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,           // đường dẫn hoặc URL ảnh đại diện (nullable)

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis() // thời điểm tạo tài khoản
)
