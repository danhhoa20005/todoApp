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
import com.example.appmanagement.databinding.FragmentSignUpBinding

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    private val vm: LoginViewModel by viewModels()

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

        // ✅ Nhận email từ Bundle (SignEmailFragment gửi qua)
        val email = arguments?.getString("email") ?: ""
        binding.tvEmailDynamic.text = email

        // Back
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        // Đăng ký
        binding.btnSignUp.setOnClickListener {
            val pass  = binding.edtPassword.text?.toString()?.trim().orEmpty()
            val pass2 = binding.edtPasswordConfirm.text?.toString()?.trim().orEmpty()

            if (email.isEmpty()) {
                toast("Thiếu email")
                return@setOnClickListener
            }
            if (pass.length < 6) {
                toast("Mật khẩu phải ≥ 6 ký tự")
                return@setOnClickListener
            }
            if (pass != pass2) {
                toast("Mật khẩu nhập lại không khớp")
                return@setOnClickListener
            }

            // Gọi ViewModel để đăng ký
            val name = email.substringBefore('@') // đặt tạm tên theo email
            vm.register(name, email, pass)
        }

        // Quan sát kết quả đăng ký
        vm.registerResult.observe(viewLifecycleOwner) { st ->
            when (st) {
                "ok" -> {
                    toast("Tạo tài khoản thành công")
                    // Đúng nav_graph: SignUp → CreateWork
                    findNavController().navigate(R.id.action_signUp_to_createWork)
                }
                "email_exists" -> toast("Email đã tồn tại")
                "invalid"      -> toast("Thiếu thông tin hoặc mật khẩu không hợp lệ")
                else           -> toast("Có lỗi, vui lòng thử lại")
            }
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
