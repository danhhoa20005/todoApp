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
import com.example.appmanagement.util.AppGlobals

// Fragment xử lý đăng nhập bằng email và mật khẩu
class SignInFragment : Fragment() {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignInViewModel by viewModels()

    // Khởi tạo view binding cho layout đăng nhập
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Thiết lập sự kiện và quan sát kết quả đăng nhập
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailFromArgs = arguments?.getString("email").orEmpty()
        binding.tvEmailDynamic.setText(emailFromArgs)

        binding.btnBack?.setOnClickListener { findNavController().popBackStack() }

        binding.btnSignIn.setOnClickListener {
            val email = binding.tvEmailDynamic.text?.toString()?.trim().orEmpty()
            val password = binding.edtPassword.text?.toString()?.trim().orEmpty()

            if (email.isEmpty() || password.isEmpty()) {
                showToast("Please enter both email and password")
                return@setOnClickListener
            }

            setLoading(true)
            viewModel.login(email, password)
        }

        viewModel.loginResult.observe(viewLifecycleOwner) { success ->
            setLoading(false)
            if (success == true) {
                AppGlobals.isLoggedIn = true
                findNavController().navigate(R.id.action_signInFragment_to_homeFragment)
            } else {
                showToast("Incorrect email or password")
            }
        }
    }

    // Khóa nút và trường nhập trong khi đang xử lý đăng nhập
    private fun setLoading(loading: Boolean) {
        binding.btnSignIn.isEnabled = !loading
        binding.edtPassword.isEnabled = !loading
        // Nếu layout có ProgressBar thì bật tắt tại đây
        // binding.progressBar.isVisible = loading
    }

    // Hiển thị thông báo lỗi đơn giản
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    // Dọn binding khi view bị huỷ
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
