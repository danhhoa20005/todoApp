// Thực thể User lưu thông tin người dùng + trạng thái đăng nhập
package com.example.appmanagement.data.entity

import androidx.room.*

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)] // email không trùng
)
data class User(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,                // id tự tăng

    @ColumnInfo(name = "name")
    val name: String,                 // tên hiển thị

    @ColumnInfo(name = "email")
    val email: String,                // email đăng nhập

    @ColumnInfo(name = "password_hash")
    val passwordHash: String,         // mật khẩu đã băm

    @ColumnInfo(name = "birth_date")
    val birthDate: String?,           // ngày sinh (nullable)

    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,           // ảnh đại diện (nullable)

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(), // thời điểm tạo

    @ColumnInfo(name = "is_logged_in")
    val isLoggedIn: Boolean = false,  // đang đăng nhập hay không

    @ColumnInfo(name = "remote_id")
    val remoteId: String? = null,     // uid Firebase (Google account)

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()  // thời điểm cập nhật gần nhất
)
