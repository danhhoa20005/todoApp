package com.example.appmanagement.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.appmanagement.data.repo.AccountRepository

/**
 * Factory để tạo instance cho CreateWorkViewModel.
 * - ViewModelProvider mặc định chỉ tạo ViewModel có constructor rỗng.
 * - Nếu ViewModel cần tham số (ở đây là AccountRepository) thì phải viết Factory riêng.
 */
class CreateWorkViewModelFactory(
    private val repo: AccountRepository // Repository được inject từ bên ngoài
) : ViewModelProvider.Factory {

    /**
     * Hàm tạo ViewModel.
     * @param modelClass class kiểu ViewModel cần tạo.
     * @return instance của CreateWorkViewModel nếu đúng loại.
     *
     * Ghi chú:
     * - isAssignableFrom: kiểm tra kiểu ViewModel yêu cầu có khớp CreateWorkViewModel không.
     * - @Suppress("UNCHECKED_CAST"): để ép kiểu an toàn khi return.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateWorkViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateWorkViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
