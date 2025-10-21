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

// Màn hình đăng ký tài khoản mới dựa trên email đã nhập trước đó
class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    // Sử dụng ViewModel mặc định không cần factory
    private val signInViewModel: SignInViewModel by viewModels()

    // Khởi tạo view binding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Thiết lập UI và quan sát kết quả đăng ký
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailFromArgs = arguments?.getString("email") ?: ""
        binding.tvEmailDynamic.text = emailFromArgs

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.btnSignUp.setOnClickListener {
            val password = binding.edtPassword.text?.toString()?.trim().orEmpty()
            val confirm  = binding.edtPasswordConfirm.text?.toString()?.trim().orEmpty()

            if (emailFromArgs.isEmpty()) { toast("Thiếu email"); return@setOnClickListener }
            if (password.length < 6) { toast("Mật khẩu phải ≥ 6 ký tự"); return@setOnClickListener }
            if (password != confirm) { toast("Mật khẩu nhập lại không khớp"); return@setOnClickListener }

            val name = emailFromArgs.substringBefore('@').ifBlank { "User" }
            signInViewModel.register(name, emailFromArgs, password)
        }

        signInViewModel.registerResult.observe(viewLifecycleOwner) { status ->
            when (status) {
                "ok" -> {
                    toast("Tạo tài khoản thành công")
                    findNavController().navigate(R.id.action_signUp_to_createWork)
                }
                "email_exists" -> toast("Email đã tồn tại")
                "invalid" -> toast("Thiếu thông tin hoặc mật khẩu không hợp lệ")
                else -> toast("Có lỗi, vui lòng thử lại")
            }
        }
    }

    // Hiển thị thông báo ngắn
    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    // Dọn binding khi view bị huỷ
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
