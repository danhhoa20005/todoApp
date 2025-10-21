package com.example.appmanagement.data.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.appmanagement.data.entity.User
import com.example.appmanagement.data.repo.AccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ViewModel quản lý thông tin người dùng cho màn hình tạo công việc
class CreateWorkViewModel(
    private val accountRepository: AccountRepository
) : ViewModel() {

    // LiveData lưu thông tin người dùng hiện tại
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> get() = _currentUser

    // LiveData theo dõi trạng thái lưu hồ sơ
    private val _isSaveSuccessful = MutableLiveData(false)
    val isSaveSuccessful: LiveData<Boolean> get() = _isSaveSuccessful

    // Tải thông tin người dùng hiện đang đăng nhập
    fun loadUser() {
        viewModelScope.launch {
            val user = withContext(Dispatchers.IO) { accountRepository.getCurrentUser() }
            _currentUser.postValue(user)
        }
    }

    // Cập nhật hồ sơ người dùng với tuỳ chọn avatar theo key
    fun updateProfile(name: String, birthDate: String, avatarKey: String?) {
        viewModelScope.launch {
            val current = _currentUser.value ?: withContext(Dispatchers.IO) {
                accountRepository.getCurrentUser()
            }

            if (current == null) {
                _isSaveSuccessful.postValue(false)
                return@launch
            }

            val finalAvatarKey = avatarKey ?: current.avatarUrl

            val updateSuccessful = try {
                withContext(Dispatchers.IO) {
                    accountRepository.updateProfile(
                        id = current.id,
                        name = name,
                        birthDate = birthDate,
                        avatarUrl = finalAvatarKey
                    )
                }
                true
            } catch (_: Exception) {
                false
            }

            if (updateSuccessful) {
                _currentUser.postValue(
                    current.copy(
                        name = name,
                        birthDate = birthDate,
                        avatarUrl = finalAvatarKey
                    )
                )
            }

            _isSaveSuccessful.postValue(updateSuccessful)
        }
    }

    // Reset cờ lưu thành công sau khi hiển thị thông báo
    fun resetSuccess() {
        _isSaveSuccessful.postValue(false)
    }

    // Factory tạo ViewModel với tham số AccountRepository
    companion object {
        fun provideFactory(accountRepository: AccountRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(CreateWorkViewModel::class.java)) {
                        return CreateWorkViewModel(accountRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}
