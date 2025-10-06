package com.example.appmanagement.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.appmanagement.data.db.AppDatabase
import com.example.appmanagement.data.repo.AccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignInViewModel(app: Application) : AndroidViewModel(app) {

    // Tầng dữ liệu
    private val db by lazy { AppDatabase.Companion.getInstance(app) }
    private val repo by lazy { AccountRepository(db.userDao()) }

    // Kết quả đăng nhập: true = thành công, false = thất bại
    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> get() = _loginResult

    // Kết quả đăng ký: "ok" | "email_exists" | "invalid" | "failed"
    private val _registerResult = MutableLiveData<String>()
    val registerResult: LiveData<String> get() = _registerResult

    fun login(email: String, matKhau: String) {
        viewModelScope.launch {
            val thanhCong = withContext(Dispatchers.IO) {
                repo.login(email, matKhau)
            }
            _loginResult.value = thanhCong
        }
    }

    fun register(ten: String, email: String, matKhau: String) {
        viewModelScope.launch {
            val ketQua = withContext(Dispatchers.IO) {
                try {
                    repo.register(ten, email, matKhau)
                    "ok"
                } catch (e: IllegalStateException) {
                    e.message ?: "email_exists"
                } catch (_: IllegalArgumentException) {
                    "invalid"
                } catch (_: Throwable) {
                    "failed"
                }
            }
            _registerResult.value = ketQua
        }
    }
}