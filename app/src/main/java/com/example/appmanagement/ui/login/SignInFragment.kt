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
import com.example.appmanagement.databinding.FragmentSignInBinding

class SignInFragment : Fragment() {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    private val vm: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ Nhận email từ Bundle (SignEmailFragment gửi sang)
        val email = arguments?.getString("email") ?: ""
        // Ví dụ: gán vào EditText để người dùng thấy luôn
        binding.tvEmailDynamic.setText(email)

        // Nút quay lại
        binding.btnBack?.setOnClickListener {
            findNavController().popBackStack()
        }

        // Đăng nhập
        binding.btnSignIn.setOnClickListener {
            val pass = binding.edtPassword.text?.toString()?.trim() ?: ""
            val inputEmail = binding.tvEmailDynamic.text?.toString()?.trim() ?: ""  // lấy từ EditText
            if (inputEmail.isEmpty() || pass.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            vm.login(inputEmail, pass)
        }

        // Quan sát kết quả đăng nhập
        vm.loginResult.observe(viewLifecycleOwner) { ok ->
            if (ok == true) {
                // Đúng nav_graph: SignIn → CreateWork
                findNavController().navigate(R.id.action_signInFragment_to_homeFragment)
            } else {
                Toast.makeText(requireContext(), "Sai email hoặc mật khẩu", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
