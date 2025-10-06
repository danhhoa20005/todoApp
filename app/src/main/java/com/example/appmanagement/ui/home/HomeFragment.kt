package com.example.appmanagement.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.appmanagement.R
import com.example.appmanagement.data.db.AppDatabase
import com.example.appmanagement.data.repo.AccountRepository
import com.example.appmanagement.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val b get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getInstance(requireContext())
        val repo = AccountRepository(db.userDao())

        // --- Lấy user đang đăng nhập (chạy IO → cập nhật UI trên Main)
        viewLifecycleOwner.lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) { repo.getCurrentUser() }

            user?.let { u ->
                // Lời chào
                b.tvHello.text = "${u.name}'s manager"

                // Ảnh đại diện
                when (u.avatarUrl) {
                    "male" -> b.imgAvatar.setImageResource(R.drawable.avatar_male)
                    "female" -> b.imgAvatar.setImageResource(R.drawable.avatar_female)
                    else -> b.imgAvatar.setImageResource(R.drawable.ic_logo)
                }

                // ✅ Khi bấm avatar → sang SettingFragment, truyền userId qua Safe Args
                b.imgAvatar.setOnClickListener {
                    val action =
                        HomeFragmentDirections.actionHomeFragmentToSettingFragment(userId = u.id)
                    findNavController().navigate(action)
                }

            } ?: run {
                // Nếu chưa đăng nhập
                b.tvHello.text = "Guest"
                b.imgAvatar.setImageResource(R.drawable.ic_logo)
            }
        }

        // --- Dữ liệu demo: Priority Progress
        val done = 3
        val total = 5
        val percent = (done.toFloat() / total * 100f)
        b.tvPrioritySub.text = "$done/$total is completed"
        b.progress.max = 100
        b.progress.setProgress(percent.toInt(), true)
        b.tvPercent.text = String.format("%.2f%%", percent)

        // --- Dữ liệu demo 3 thẻ
        b.tvTotalTask.text = "16"
        b.tvCompleted.text = "32"
        b.tvProjects.text = "8"

        // --- Click card → Toast
        b.cardTotalTask.setOnClickListener { toast("Open: Total Task") }
        b.cardCompleted.setOnClickListener { toast("Open: Completed") }
        b.cardProjects.setOnClickListener { toast("Open: Total Projects") }
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
