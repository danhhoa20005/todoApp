// Fragment SettingFragment hiển thị thông tin người dùng và cho phép đăng xuất khỏi ứng dụng
package com.example.appmanagement.ui.menu

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.appmanagement.R
import com.example.appmanagement.data.db.AppDatabase
import com.example.appmanagement.data.viewmodel.TaskViewModel
import com.example.appmanagement.databinding.FragmentSettingBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import com.example.appmanagement.util.AppGlobals
import com.example.appmanagement.util.ThemeManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

import kotlinx.coroutines.launch

class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val b get() = _binding!!

    private val args: SettingFragmentArgs by navArgs()
    private val taskViewModel: TaskViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        val db = AppDatabase.getInstance(context)
        val userDao = db.userDao()

        // Lấy user theo userId (suspend) rồi đổ UI
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val u = userDao.getById(args.userId)   // Lấy thông tin người dùng theo đối số truyền vào
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

        val isDarkMode = ThemeManager.isDarkMode(context)
        updateModeLabel(isDarkMode)
        b.switchMode.isChecked = isDarkMode
        b.switchMode.setOnCheckedChangeListener { _, checked ->
            ThemeManager.setDarkMode(context, checked)
            updateModeLabel(checked)
        }

        // Logout -> Onboard (đã có action trong nav_graph)
        b.btnLogout.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                // xóa cờ is_logged_in trong DB
                withContext(Dispatchers.IO) {
                    AppDatabase.getInstance(requireContext()).userDao().clearLoggedIn()
                }

                // sign out khỏi Google/Firebase để luôn hiện hộp chọn tài khoản khi đăng nhập lại
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                GoogleSignIn.getClient(requireContext(), gso).signOut()
                FirebaseAuth.getInstance().signOut()
                taskViewModel.clearSession()

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

    private fun updateModeLabel(isDark: Boolean) {
        val labelRes = if (isDark) R.string.setting_dark_mode else R.string.setting_light_mode
        b.tvModeLabel.setText(labelRes)
    }
}
