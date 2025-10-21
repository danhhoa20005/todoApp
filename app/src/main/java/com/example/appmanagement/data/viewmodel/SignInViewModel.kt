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

// ViewModel xử lý luồng đăng nhập và đăng ký tài khoản
class SignInViewModel(app: Application) : AndroidViewModel(app) {

    // Truy cập tầng dữ liệu
    private val database by lazy { AppDatabase.getInstance(app) }
    private val accountRepository by lazy { AccountRepository(database.userDao()) }

    // Kết quả đăng nhập: true thành công, false thất bại
    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> get() = _loginResult

    // Kết quả đăng ký: ok, email_exists, invalid, failed
    private val _registerResult = MutableLiveData<String>()
    val registerResult: LiveData<String> get() = _registerResult

    // Đăng nhập với email và mật khẩu, repository sẽ kiểm tra BCrypt
    fun login(email: String, password: String) {
        val e = email.trim()
        val p = password.trim()
        if (e.isEmpty() || p.isEmpty()) {
            _loginResult.value = false
            return
        }

        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                accountRepository.login(e, p)
            }
            _loginResult.value = success
        }
    }

    // Đăng ký người dùng mới, repository tự băm mật khẩu
    fun register(name: String, email: String, password: String) {
        val n = name.trim()
        val e = email.trim()
        val p = password.trim()

        if (n.isEmpty() || e.isEmpty() || p.length < 6) {
            _registerResult.value = "invalid"
            return
        }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    accountRepository.register(n, e, p)
                    "ok"
                } catch (ise: IllegalStateException) {
                    // Repository trả về email_exists hoặc invalid_input
                    when (ise.message) {
                        "email_exists"  -> "email_exists"
                        "invalid_input" -> "invalid"
                        else            -> "failed"
                    }
                } catch (_: Throwable) {
                    "failed"
                }
            }
            _registerResult.value = result
        }
    }
}
