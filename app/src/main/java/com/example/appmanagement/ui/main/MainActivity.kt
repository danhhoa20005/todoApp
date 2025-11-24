// MainActivity khởi tạo giao diện chính, điều hướng BottomNavigation
// và xin các quyền cần thiết (thông báo + exact alarm)
package com.example.appmanagement.ui.main

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.appmanagement.R
import com.example.appmanagement.databinding.ActivityMainBinding
import com.example.appmanagement.util.ThemeManager
import com.google.android.material.navigation.NavigationBarView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Áp dụng theme đã lưu (Dark/Sáng)
        ThemeManager.applySavedTheme(this)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Áp padding cho root theo system bars (status + navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Lấy NavController từ NavHostFragment
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Các màn hình top-level (hiện BottomNavigation)
        val topLevelDestinations = setOf(
            R.id.homeFragment,
            R.id.addFragment,
            R.id.listFragment,
            R.id.calendarFragment,
            R.id.clockFragment,
            R.id.doneFragment,
            R.id.todayFragment,
            R.id.dateDetailFragment,
            R.id.editFragment
        )

        // Gắn NavigationController cho BottomNavigationView
        binding.bottomNav.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        binding.bottomNav.setupWithNavController(navController)

        // Ẩn BottomNavigation ở các màn hình không thuộc top-level
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility =
                if (destination.id in topLevelDestinations) View.VISIBLE else View.GONE
        }

        // Xin các quyền cần thiết
        requestNotificationPermissionIfNeeded()
        requestExactAlarmPermissionIfNeeded()
    }

    // Launcher xin quyền POST_NOTIFICATIONS (Android 13+)
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Có thể xử lý kết quả nếu muốn, ở đây bỏ qua
        }

    // Xin quyền thông báo (POST_NOTIFICATIONS) cho Android 13+
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val permission = Manifest.permission.POST_NOTIFICATIONS
        val granted = ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            requestNotificationPermissionLauncher.launch(permission)
        }
    }

    // Kiểm tra và yêu cầu quyền exact alarm (Android 12+)
    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Nếu đã có quyền schedule exact alarms thì bỏ qua
        if (alarmManager.canScheduleExactAlarms()) return

        // Mở màn hình cài đặt để user cấp quyền sử dụng exact alarm cho app
        try {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = android.net.Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (_: Exception) {
            // Fallback: mở màn hình chi tiết ứng dụng nếu action trên không hỗ trợ
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (_: Exception) {
                // Bó tay, không làm gì thêm
            }
        }
    }
}
