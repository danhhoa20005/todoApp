// Đối tượng tiện ích lưu trạng thái chung về đăng nhập và người dùng hiện tại
package com.example.appmanagement.utils

// Chia sẻ trạng thái đăng nhập cho toàn bộ ứng dụng
object AppGlobals {
    // Ghi nhận người dùng đã đăng nhập hay chưa
    var isLoggedIn: Boolean = false
    // Lưu id người dùng hiện tại để tái sử dụng
    var currentUserId: Long? = null
}
