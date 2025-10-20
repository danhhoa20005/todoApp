// Đối tượng toàn cục giữ thông tin phiên đăng nhập hiện hành được các thành phần khác truy cập
package com.example.appmanagement.util

object AppGlobals {
    var isLoggedIn: Boolean = false
    var currentUserId: Long? = null
}
