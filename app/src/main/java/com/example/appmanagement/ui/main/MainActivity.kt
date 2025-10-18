package com.example.appmanagement.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.appmanagement.R
import com.example.appmanagement.databinding.ActivityMainBinding
import com.example.appmanagement.util.TaskReminderScheduler
import com.google.android.material.navigation.NavigationBarView

private const val REQUEST_POST_NOTIFICATIONS = 1001

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        TaskReminderScheduler.ensureChannel(applicationContext)
        requestNotificationPermissionIfNeeded()

        // Áp padding cho root theo system bars (status + navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Lấy NavController từ NavHostFragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Các màn hình top-level (tức là các tab trong BottomNavigation)
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
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) return

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            Toast.makeText(this, R.string.task_notification_permission_rationale, Toast.LENGTH_LONG).show()
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(permission),
            REQUEST_POST_NOTIFICATIONS
        )
    }
}
