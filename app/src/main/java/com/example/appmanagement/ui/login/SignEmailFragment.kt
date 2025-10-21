package com.example.appmanagement.ui.login

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.appmanagement.R
import com.example.appmanagement.data.db.AppDatabase
import com.example.appmanagement.databinding.FragmentSignEmailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Bước nhập email kiểm tra tài khoản tồn tại để điều hướng đăng nhập hoặc đăng ký
class SignEmailFragment : Fragment() {

    private var _binding: FragmentSignEmailBinding? = null
    private val binding get() = _binding!!

    // Tạo view binding cho fragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Thiết lập sự kiện cho các nút sau khi view được tạo
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnContinue.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()
            if (!isValidEmail(email)) {
                Toast.makeText(requireContext(), "Email không hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val userDao = AppDatabase.getInstance(requireContext()).userDao()
                val exists = withContext(Dispatchers.IO) {
                    userDao.existsEmail(email)
                }

                val args = bundleOf("email" to email)

                if (exists) {
                    findNavController().navigate(R.id.action_signEmail_to_signIn, args)
                } else {
                    findNavController().navigate(R.id.action_signEmail_to_signUp, args)
                }
            }
        }
    }

    // Kiểm tra định dạng email bằng hằng số của Android
    private fun isValidEmail(s: CharSequence?): Boolean =
        !s.isNullOrBlank() && Patterns.EMAIL_ADDRESS.matcher(s).matches()

    // Giải phóng binding để tránh rò rỉ bộ nhớ
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
