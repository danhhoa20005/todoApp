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
 * - Giữ logic gọi Repository (AccountRepository)
 * - Expose LiveData cho UI quan sát và phản ứng (MVVM)
 */
class LoginViewModel(app: Application) : AndroidViewModel(app) {

    // ---------------------------
    // Khởi tạo tầng data
    // ---------------------------


    private val db by lazy { AppDatabase.getInstance(app) }

    // Quản lý trạng thái đăng nhập + thông tin user bằng DataStore
    private val prefs by lazy { UserPreferences(app) }

    // Repository trung gian giữa ViewModel ↔ DAO + DataStore
    private val repo by lazy { AccountRepository(db.userDao(), prefs) }

    // ---------------------------
    // LiveData cho UI quan sát
    // ---------------------------

    // Kết quả đăng nhập: true = thành công, false = thất bại
    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> = _loginResult

    // Kết quả đăng ký:
    // "ok"           → thành công
    // "email_exists" → trùng email
    // "invalid"      → input không hợp lệ
    // "failed"       → lỗi khác (DB, hệ thống…)
    private val _registerResult = MutableLiveData<String>()
    val registerResult: LiveData<String> = _registerResult

    // ---------------------------
    // Hành động từ UI gọi xuống
    // ---------------------------

    /** Đăng nhập */
    fun login(email: String, pass: String) = viewModelScope.launch {
        val ok = repo.login(email, pass)
        // postValue để gửi kết quả sang UI (có thể quan sát trong Fragment/Activity)
        _loginResult.postValue(ok)
    }

    /** Đăng ký */
    fun register(name: String, email: String, pass: String) = viewModelScope.launch {
        try {
            repo.register(name, email, pass)   // gọi xuống Repository
            _registerResult.postValue("ok")
        } catch (e: IllegalStateException) {
            // Repo ném khi email đã tồn tại
            _registerResult.postValue(e.message ?: "email_exists")
        } catch (_: IllegalArgumentException) {
            // Repo ném khi input thiếu hoặc không hợp lệ
            _registerResult.postValue("invalid")
        } catch (_: Throwable) {
            // Các lỗi khác (DB, hệ thống…)
            _registerResult.postValue("failed")
        }
    }
}
