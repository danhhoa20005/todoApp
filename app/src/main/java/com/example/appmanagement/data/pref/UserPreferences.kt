package com.example.appmanagement.data.pref

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Tạo DataStore tên "settings" gắn với Context (extension property)
private val Context.dataStore by preferencesDataStore(name = "settings")

// Khóa cho các giá trị lưu trữ
private object Keys {
    val loggedIn = booleanPreferencesKey("logged_in")     // đã đăng nhập?
    val currentUserId = longPreferencesKey("current_user_id") // id người dùng hiện hành
}

class UserPreferences(private val context: Context) {

    // Flow<Boolean>: true nếu đã đăng nhập, false nếu chưa
    val isLoggedInFlow: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.loggedIn] ?: false }

    // Flow<Long>: id người dùng hiện hành; -1 nếu chưa có
    val currentUserIdFlow: Flow<Long> =
        context.dataStore.data.map { it[Keys.currentUserId] ?: -1L }

    // Lưu trạng thái đăng nhập
    suspend fun setLoggedIn(value: Boolean) {
        context.dataStore.edit { it[Keys.loggedIn] = value }
    }

    // Lưu id người dùng hiện hành
    suspend fun setCurrentUserId(id: Long) {
        context.dataStore.edit { it[Keys.currentUserId] = id }
    }

    // Xoá trạng thái (đăng xuất)
    suspend fun clear() {
        context.dataStore.edit {
            it.remove(Keys.loggedIn)
            it.remove(Keys.currentUserId)
        }
    }
}
