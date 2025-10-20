// Đối tượng tiện ích lưu trạng thái chung về đăng nhập và người dùng hiện tại
package com.example.appmanagement.utils

object AppGlobals {
    var isLoggedIn: Boolean = false
    var currentUserId: Long? = null
}
