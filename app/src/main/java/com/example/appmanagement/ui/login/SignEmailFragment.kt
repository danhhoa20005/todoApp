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

// SignEmailFragment
// -----------------
// Mục đích:
// - Bước nhập email khi đăng nhập/đăng ký.
// - Kiểm tra email có hợp lệ và có tồn tại trong DB hay không.
// - Nếu có → chuyển sang SignIn, nếu chưa → chuyển sang SignUp.
//
class SignEmailFragment : Fragment() {

    private var _binding: FragmentSignEmailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Nút back → quay lại màn trước
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Nút Continue → kiểm tra email
        binding.btnContinue.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()
            if (!isValidEmail(email)) {
                Toast.makeText(requireContext(), "Email không hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Kiểm tra email trong DB
            lifecycleScope.launch {
                val userDao = AppDatabase.getInstance(requireContext()).userDao()
                val exists = withContext(Dispatchers.IO) {
                    userDao.existsEmail(email)
                }

                val args = bundleOf("email" to email)

                if (exists) {
                    // Email đã có → sang màn SignIn
                    findNavController().navigate(R.id.action_signEmail_to_signIn, args)
                } else {
                    // Email chưa có → sang màn SignUp
                    findNavController().navigate(R.id.action_signEmail_to_signUp, args)
                }
            }
        }
    }

    // Hàm validate email (regex từ Android Patterns)
    private fun isValidEmail(s: CharSequence?): Boolean =
        !s.isNullOrBlank() && Patterns.EMAIL_ADDRESS.matcher(s).matches()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // tránh leak bộ nhớ
    }
}
