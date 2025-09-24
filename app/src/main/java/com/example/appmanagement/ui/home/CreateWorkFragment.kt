package com.example.appmanagement.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.appmanagement.R
import com.example.appmanagement.databinding.FragmentCreateWorkBinding

/**
 * Màn hình chính (Home) sau khi đăng nhập
 * - layout: fragment_create_work.xml  -> sinh ra FragmentCreateWorkBinding
 */
class CreateWorkFragment : Fragment() {

    private var _binding: FragmentCreateWorkBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateWorkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ví dụ: xử lý click "Thêm task mới"
        binding.btnAddTask.setOnClickListener {
            // Điều hướng sang màn tạo task (nếu có trong nav_graph)
            // findNavController().navigate(R.id.action_createWorkFragment_to_taskAddFragment)
        }

        // Ví dụ: xử lý click "Đăng xuất"
        binding.btnLogout.setOnClickListener {
            // Điều hướng về Onboard / SignIn
            findNavController().navigate(R.id.onboardFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
