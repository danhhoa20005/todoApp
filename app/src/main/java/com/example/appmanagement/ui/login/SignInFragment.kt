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
import com.example.appmanagement.databinding.FragmentSignInBinding
import com.example.appmanagement.utils.AppGlobals


// SignInFragment
// --------------
// Mục đích:
// - Màn hình đăng nhập sau khi người dùng nhập email từ SignEmailFragment.
// - Người dùng nhập mật khẩu, hệ thống gọi LoginViewModel để xác thực.
// - Nếu đúng → điều hướng sang HomeFragment.
// - Nếu sai → hiển thị thông báo lỗi.
//
class SignInFragment : Fragment() {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    // ViewModel quản lý logic đăng nhập
    private val signInViewModel: SignInViewModel by viewModels()

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

        // Lấy email từ Bundle (truyền từ SignEmailFragment)
        val emailFromArgs = arguments?.getString("email") ?: ""
        binding.tvEmailDynamic.setText(emailFromArgs)

        // Nút quay lại
        binding.btnBack?.setOnClickListener {
            findNavController().popBackStack()
        }

        // Nút đăng nhập
        binding.btnSignIn.setOnClickListener {
            val passwordInput = binding.edtPassword.text?.toString()?.trim() ?: ""
            val emailInput = binding.tvEmailDynamic.text?.toString()?.trim() ?: ""

            if (emailInput.isEmpty() || passwordInput.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Gọi ViewModel để xác thực
            signInViewModel.login(emailInput, passwordInput)
        }

        // Quan sát kết quả đăng nhập
        signInViewModel.loginResult.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess == true) {
                // Thành công → điều hướng sang HomeFragment
                AppGlobals.isLoggedIn = true
                findNavController().navigate(R.id.action_signInFragment_to_homeFragment)
            } else {
                // Thất bại → thông báo
                Toast.makeText(requireContext(), "Sai email hoặc mật khẩu", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Giải phóng binding tránh leak bộ nhớ
    }
}
