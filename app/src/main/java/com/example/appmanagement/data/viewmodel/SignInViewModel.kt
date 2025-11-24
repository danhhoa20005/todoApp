// ViewModel SignInViewModel xử lý đăng nhập và đăng ký bằng cách gọi AccountRepository trong coroutine
package com.example.appmanagement.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.appmanagement.data.entity.User
import com.example.appmanagement.data.repo.AccountRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    // Login result: true = success, false = failure
    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> get() = _loginResult

    // Register result: "ok" | "email_exists" | "invalid" | "failed"
    private val _registerResult = MutableLiveData<String>()
    val registerResult: LiveData<String> get() = _registerResult

    // login – đăng nhập email + password local
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

    // register – tạo tài khoản local mới
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

    // loginWithGoogleUser – nhận FirebaseUser – gọi repo.loginWithGoogleAccount – trả User về callback
    fun loginWithGoogleUser(
        firebaseUser: FirebaseUser,
        onSuccess: (User, Boolean) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    accountRepository.loginWithGoogleAccount(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        name = firebaseUser.displayName,
                        avatarUrl = firebaseUser.photoUrl?.toString()
                    )
                }
                onSuccess(result.user, result.isNewUser)
            } catch (e: Throwable) {
                onError(e)
            }
        }
    }
}
