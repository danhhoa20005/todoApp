package com.example.appmanagement.data.pref

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Tạo DataStore tên "settings" gắn vào Context.
// Đây là extension property cho Context, mỗi app chỉ nên có 1 instance.
private val Context.dataStore by preferencesDataStore(name = "settings")

// Định nghĩa các "key" dùng để lưu/đọc giá trị trong DataStore.
// Mỗi key ứng với một cột dữ liệu đơn giản (boolean, string, long…).
private object Keys {
    val loggedIn = booleanPreferencesKey("logged_in")           // trạng thái đăng nhập
    val currentUserId = longPreferencesKey("current_user_id")   // id user hiện hành

    // Thông tin cơ bản user
    val userName = stringPreferencesKey("user_name")
    val userEmail = stringPreferencesKey("user_email")
    val userAvatar = stringPreferencesKey("user_avatar")
    val userBirthDate = stringPreferencesKey("user_birth_date")
}

/**
 * Lớp wrapper để thao tác với DataStore liên quan đến User.
 * - Dùng để lấy trạng thái đăng nhập và thông tin user theo dạng Flow.
 * - Dùng để cập nhật hoặc xoá thông tin khi đăng nhập/đăng xuất.
 */
class UserPreferences(private val context: Context) {

    // -----------------------------
    // Các Flow để quan sát dữ liệu
    // -----------------------------

    // Flow trạng thái đăng nhập (true/false). Mặc định false nếu chưa có dữ liệu.
    val isLoggedInFlow: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.loggedIn] ?: false }

    // Flow id user hiện tại. Mặc định -1L nếu chưa đăng nhập.
    val currentUserIdFlow: Flow<Long> =
        context.dataStore.data.map { it[Keys.currentUserId] ?: -1L }

    // Flow tên user
    val userNameFlow: Flow<String> =
        context.dataStore.data.map { it[Keys.userName] ?: "" }

    // Flow email user
    val userEmailFlow: Flow<String> =
        context.dataStore.data.map { it[Keys.userEmail] ?: "" }

    // Flow avatar user (link hoặc path local). Mặc định chuỗi rỗng.
    val userAvatarFlow: Flow<String> =
        context.dataStore.data.map { it[Keys.userAvatar] ?: "" }

    // Flow ngày sinh user. Mặc định chuỗi rỗng.
    val userBirthDateFlow: Flow<String> =
        context.dataStore.data.map { it[Keys.userBirthDate] ?: "" }

    // -----------------------------
    // Các hàm cập nhật dữ liệu
    // -----------------------------

    // Lưu trạng thái đăng nhập (true/false).
    suspend fun setLoggedIn(value: Boolean) {
        context.dataStore.edit { it[Keys.loggedIn] = value }
    }

    // Lưu id user hiện hành (khi login/register thành công).
    suspend fun setCurrentUserId(id: Long) {
        context.dataStore.edit { it[Keys.currentUserId] = id }
    }

    // Lưu toàn bộ thông tin cơ bản của user (dùng khi login/register).
    suspend fun setUserInfo(
        name: String,
        email: String,
        avatar: String,
        birthDate: String
    ) {
        context.dataStore.edit {
            it[Keys.userName] = name
            it[Keys.userEmail] = email
            it[Keys.userAvatar] = avatar
            it[Keys.userBirthDate] = birthDate
        }
    }

    // Xoá toàn bộ dữ liệu (dùng khi logout).
    suspend fun clear() {
        context.dataStore.edit {
            it.remove(Keys.loggedIn)
            it.remove(Keys.currentUserId)
            it.remove(Keys.userName)
            it.remove(Keys.userEmail)
            it.remove(Keys.userAvatar)
            it.remove(Keys.userBirthDate)
        }
    }
}
