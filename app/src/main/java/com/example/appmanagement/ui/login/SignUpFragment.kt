package com.example.appmanagement.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.appmanagement.R
import com.example.appmanagement.data.viewmodel.SignInViewModel
import com.example.appmanagement.databinding.FragmentSignUpBinding

// SignUpFragment
// --------------
// Mục đích:
// - Màn hình đăng ký tài khoản mới.
// - Người dùng nhập mật khẩu, xác nhận lại mật khẩu.
// - Nếu hợp lệ → gọi LoginViewModel để tạo tài khoản.
// - Thành công → chuyển sang CreateWorkFragment để bổ sung thông tin.
//
class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    // ViewModel quản lý logic đăng nhập/đăng ký
    private val signInViewModel: SignInViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Nhận email từ Bundle (SignEmailFragment gửi qua)
        val emailFromArgs = arguments?.getString("email") ?: ""
        binding.tvEmailDynamic.text = emailFromArgs

        // Nút quay lại
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        // Nút Đăng ký
        binding.btnSignUp.setOnClickListener {
            val passwordInput = binding.edtPassword.text?.toString()?.trim().orEmpty()
            val confirmPasswordInput = binding.edtPasswordConfirm.text?.toString()?.trim().orEmpty()

            if (emailFromArgs.isEmpty()) {
                showToast("Thiếu email")
                return@setOnClickListener
            }
            if (passwordInput.length < 6) {
                showToast("Mật khẩu phải ≥ 6 ký tự")
                return@setOnClickListener
            }
            if (passwordInput != confirmPasswordInput) {
                showToast("Mật khẩu nhập lại không khớp")
                return@setOnClickListener
            }

            // Gọi ViewModel để đăng ký (tạm đặt name = phần trước @ của email)
            val name = emailFromArgs.substringBefore('@')
            signInViewModel.register(name, emailFromArgs, passwordInput)
        }

        // Quan sát kết quả đăng ký
        signInViewModel.registerResult.observe(viewLifecycleOwner) { status ->
            when (status) {
                "ok" -> {
                    showToast("Tạo tài khoản thành công")
                    findNavController().navigate(R.id.action_signUp_to_createWork)
                }
                "email_exists" -> showToast("Email đã tồn tại")
                "invalid" -> showToast("Thiếu thông tin hoặc mật khẩu không hợp lệ")
                else -> showToast("Có lỗi, vui lòng thử lại")
            }
        }
    }

    // Hàm hiển thị Toast
    private fun showToast(message: String) =
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
