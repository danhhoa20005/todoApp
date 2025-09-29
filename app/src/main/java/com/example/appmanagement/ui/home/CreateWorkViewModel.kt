package com.example.appmanagement.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appmanagement.data.entity.User
import com.example.appmanagement.data.repo.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel cho màn hình "CreateWork" (hoặc trang Home có liên quan đến user).
 * - Nhiệm vụ: quản lý dữ liệu user và cập nhật profile.
 * - Sử dụng StateFlow để UI có thể "observe" trạng thái (thay thế LiveData).
 */
class CreateWorkViewModel(
    private val repo: AccountRepository // inject repository để gọi xuống DB + DataStore
) : ViewModel() {

    // -----------------------------
    // State quản lý dữ liệu
    // -----------------------------

    // Dữ liệu user hiện tại. Bắt đầu là null, sau khi load thì có giá trị.
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user   // expose cho UI: chỉ đọc, không chỉnh sửa trực tiếp

    // Trạng thái báo hiệu thành công (ví dụ khi update profile).
    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success

    // -----------------------------
    // Các hành động nghiệp vụ
    // -----------------------------

    /**
     * Load thông tin user hiện tại từ Repository.
     * - Gọi khi UI cần hiển thị dữ liệu (ví dụ khi mở màn hình).
     */
    fun loadUser() {
        viewModelScope.launch {
            _user.value = repo.getCurrentUser()
        }
    }

    /**
     * Cập nhật profile của user.
     * - Nếu _user đã có → dùng luôn.
     * - Nếu chưa có (null) → gọi lại repo.getCurrentUser().
     * - Sau khi cập nhật thành công → gán _success = true để UI biết.
     */
    fun updateProfile(name: String, birthDate: String, avatarUrl: String?) {
        viewModelScope.launch {
            val u = _user.value ?: repo.getCurrentUser()
            if (u != null) {
                repo.updateProfile(u.id, name, birthDate, avatarUrl)
                _success.value = true
            }
        }
    }
}
