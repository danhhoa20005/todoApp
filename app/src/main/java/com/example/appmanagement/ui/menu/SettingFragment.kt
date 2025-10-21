package com.example.appmanagement.ui.menu

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.appmanagement.R
import com.example.appmanagement.data.db.AppDatabase
import com.example.appmanagement.databinding.FragmentSettingBinding
import com.example.appmanagement.util.AppGlobals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Màn hình cài đặt hiển thị thông tin người dùng và xử lý đăng xuất
class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val b get() = _binding!!

    private val args: SettingFragmentArgs by navArgs()

    // Khởi tạo binding cho layout setting
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return b.root
    }

    // Tải thông tin người dùng và thiết lập hành động cho các nút
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getInstance(requireContext())
        val userDao = db.userDao()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val u = userDao.getById(args.userId)
            withContext(Dispatchers.Main) {
                if (u == null) {
                    b.tvName.text = "Unknown User"
                    b.tvEmail.text = ""
                    b.imgAvatar.setImageResource(R.drawable.ic_logo)
                    return@withContext
                }
                b.tvName.text = u.name
                b.tvEmail.text = u.email

                when (u.avatarUrl) {
                    "male" -> b.imgAvatar.setImageResource(R.drawable.avatar_male)
                    "female" -> b.imgAvatar.setImageResource(R.drawable.avatar_female)
                    else -> {
                        u.avatarUrl
                            ?.takeIf { it.isNotBlank() }
                            ?.let { safe ->
                                try {
                                    val uri = Uri.parse(safe)
                                    b.imgAvatar.setImageURI(uri)
                                } catch (_: Exception) {
                                    b.imgAvatar.setImageResource(R.drawable.ic_logo)
                                }
                            } ?: b.imgAvatar.setImageResource(R.drawable.ic_logo)
                    }
                }
            }
        }

        b.btnBack.setOnClickListener { findNavController().navigateUp() }

        b.btnLogout.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    AppDatabase.getInstance(requireContext()).userDao().clearLoggedIn()
                }

                AppGlobals.isLoggedIn = false
                AppGlobals.currentUserId = null

                findNavController().navigate(
                    R.id.onboardFragment,
                    null,
                    androidx.navigation.navOptions {
                        popUpTo(R.id.plashOnboardFragment) { inclusive = true }
                    }
                )
            }
        }
    }

    // Dọn binding khi view bị huỷ
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
