package com.example.appmanagement.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.appmanagement.data.db.AppDatabase
import com.example.appmanagement.data.pref.UserPreferences
import com.example.appmanagement.data.repo.AccountRepository
import kotlinx.coroutines.launch

/**
 * ViewModel cho luồng đăng nhập/đăng ký.
 * - Giữ logic gọi Repository
 * - Expose LiveData cho UI quan sát
 */
class LoginViewModel(app: Application) : AndroidViewModel(app) {

    // Tầng data: khởi tạo bằng Application để có Context an toàn
    private val db by lazy { AppDatabase.getInstance(app) }   // ✅ đổi sang getInstance(...)
    private val prefs by lazy { UserPreferences(app) }
    private val repo by lazy { AccountRepository(db.userDao(), prefs) }

    // Kết quả đăng nhập: true=thành công, false=thất bại
    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> = _loginResult

    // Kết quả đăng ký: "ok" | "email_exists" | "invalid" | "failed"
    private val _registerResult = MutableLiveData<String>()
    val registerResult: LiveData<String> = _registerResult

    /** Đăng nhập */
    fun login(email: String, pass: String) = viewModelScope.launch {
        val ok = repo.login(email, pass)
        _loginResult.postValue(ok)
    }

    /** Đăng ký */
    fun register(name: String, email: String, pass: String) = viewModelScope.launch {
        try {
            repo.register(name, email, pass)
            _registerResult.postValue("ok")
        } catch (e: IllegalStateException) {           // repository ném khi email đã tồn tại
            _registerResult.postValue(e.message ?: "email_exists")
        } catch (_: IllegalArgumentException) {        // tham số thiếu/không hợp lệ
            _registerResult.postValue("invalid")
        } catch (_: Throwable) {                       // lỗi khác
            _registerResult.postValue("failed")
        }
    }
}
