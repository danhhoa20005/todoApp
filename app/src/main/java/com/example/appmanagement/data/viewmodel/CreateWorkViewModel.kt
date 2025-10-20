// ViewModel CreateWorkViewModel phụ trách tải và cập nhật hồ sơ người dùng phục vụ màn hình cài đặt
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

class CreateWorkViewModel(
    private val accountRepository: AccountRepository
) : ViewModel() {

    // LiveData lưu thông tin người dùng hiện tại
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> get() = _currentUser

    // LiveData lưu trạng thái lưu thành công
    private val _isSaveSuccessful = MutableLiveData(false)
    val isSaveSuccessful: LiveData<Boolean> get() = _isSaveSuccessful

    // Tải thông tin người dùng hiện đang đăng nhập
    fun loadUser() {
        viewModelScope.launch {
            val user = withContext(Dispatchers.IO) { accountRepository.getCurrentUser() }
            _currentUser.postValue(user)
        }
    }

    // Cập nhật hồ sơ người dùng
    // avatarKey:
    // - "male" | "female": lưu đúng key vào DB
    // - null: giữ nguyên avatarUrl hiện tại (không ghi đè null)
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

    // Reset trạng thái lưu thành công sau khi đã hiển thị thông báo
    fun resetSuccess() {
        _isSaveSuccessful.postValue(false)
    }

    // Hàm tạo ViewModel có tham số Repository (dùng cho viewModels factory)
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
