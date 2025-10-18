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
import com.example.appmanagement.util.ThemeUtils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val b get() = _binding!!

    private val args: SettingFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getInstance(requireContext())
        val userDao = db.userDao()

        // Lấy user theo userId (suspend) rồi đổ UI
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val u = userDao.getById(args.userId)   // ⬅ đổi từ getByIdOnce() -> getById()
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
                        // Nếu có chuỗi URL/URI hợp lệ -> parse, còn lại dùng logo
                        u.avatarUrl
                            ?.takeIf { it.isNotBlank() }
                            ?.let { safe ->
                                try {
                                    val uri = Uri.parse(safe)  // safe là String, không null
                                    b.imgAvatar.setImageURI(uri)
                                } catch (_: Exception) {
                                    b.imgAvatar.setImageResource(R.drawable.ic_logo)
                                }
                            } ?: run {
                            b.imgAvatar.setImageResource(R.drawable.ic_logo)
                        }
                    }
                }
            }
        }

        // Back
        b.btnBack.setOnClickListener { findNavController().navigateUp() }

        val themeSwitch = b.switchTheme
        val isDarkModeEnabled = ThemeUtils.isDarkModeSelected(requireContext())
        themeSwitch.isChecked = isDarkModeEnabled
        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            ThemeUtils.persistDarkMode(requireContext(), isChecked)
            ThemeUtils.applyTheme(isChecked)
        }

        // Logout -> Onboard (đã có action trong nav_graph)
        b.btnLogout.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                // xóa cờ is_logged_in trong DB
                withContext(Dispatchers.IO) {
                    AppDatabase.getInstance(requireContext()).userDao().clearLoggedIn()
                }

                // reset biến toàn cục
                AppGlobals.isLoggedIn = false
                AppGlobals.currentUserId = null

                // điều hướng và dọn stack
                findNavController().navigate(
                    R.id.onboardFragment,
                    null,
                    androidx.navigation.navOptions {
                        // pop sạch về Splash để không quay lại Home/Setting được nữa
                        popUpTo(R.id.plashOnboardFragment) { inclusive = true }
                    }
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
