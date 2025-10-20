// Fragment OnboardFragment giới thiệu ứng dụng và dẫn người dùng tới bước đăng nhập đầu tiên
package com.example.appmanagement.ui.onboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.appmanagement.R
import com.example.appmanagement.databinding.FragmentOnboardBinding

// OnboardFragment
// ---------------
// Mục đích:
// - Là màn hình giới thiệu (onboarding) khi mở ứng dụng lần đầu.
// - Chỉ có một nút "Start" để chuyển sang bước nhập email (SignEmailFragment).
//
class OnboardFragment : Fragment() {

    private var _binding: FragmentOnboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Gắn layout với binding
        _binding = FragmentOnboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Nút "Start" → điều hướng sang màn nhập email
        binding.btnStart.setOnClickListener {
            findNavController().navigate(R.id.action_onboard_to_signEmail)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Giải phóng binding để tránh leak bộ nhớ
    }
}
